package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTaskDetail;
import com.sjtb.reporting.domain.ReportTaskSchedule;
import com.sjtb.reporting.domain.ReportTaskScheduleRun;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportTaskDetailRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRunRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Executes each schedule in isolated transactions so a failed task never rolls back its run audit. */
@Service
public class TaskScheduleExecutionService {
    private final ReportTaskScheduleRepository schedules;
    private final ReportTaskScheduleRunRepository runs;
    private final ReportTaskRepository tasks;
    private final ReportTaskDetailRepository details;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final AccessControlService access;

    public TaskScheduleExecutionService(ReportTaskScheduleRepository schedules, ReportTaskScheduleRunRepository runs,
                                        ReportTaskRepository tasks, ReportTaskDetailRepository details, UserRepository users,
                                        DepartmentRepository departments, AccessControlService access) {
        this.schedules = schedules; this.runs = runs; this.tasks = tasks; this.details = details;
        this.users = users; this.departments = departments; this.access = access;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(Long scheduleId, LocalDateTime requestedAt, boolean manual) {
        ReportTaskSchedule schedule = schedules.findWithLockById(scheduleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "定时发布规则不存在"));
        if (!manual && (!"ACTIVE".equals(schedule.getStatus()) || schedule.getNextRunAt().isAfter(requestedAt)
                || (schedule.getEndAt() != null && schedule.getNextRunAt().isAfter(schedule.getEndAt())))) {
            return Claim.skipped(schedule, requestedAt);
        }
        LocalDateTime publishedAt = manual ? requestedAt : schedule.getNextRunAt();
        String period = TaskScheduleTiming.periodKey(schedule, publishedAt);
        var existing = runs.findByScheduleIdAndPeriodKey(schedule.getId(), period);
        if (existing.isPresent()) {
            ReportTaskScheduleRun previous = existing.get();
            if (manual && "FAILED".equals(previous.getStatus())) {
                previous.setStatus("PROCESSING"); previous.setErrorMessage(null); previous.setPublishedAt(publishedAt); previous.setExecutedAt(LocalDateTime.now());
                return Claim.created(previous, publishedAt);
            }
            advanceCurrentPeriod(schedule, period);
            return Claim.existing(previous);
        }

        ReportTaskScheduleRun run = new ReportTaskScheduleRun();
        run.setSchedule(schedule); run.setPeriodKey(period); run.setPublishedAt(publishedAt); run.setStatus("PROCESSING");
        runs.saveAndFlush(run);
        advanceCurrentPeriod(schedule, period);
        return Claim.created(run, publishedAt);
    }

    private void advanceCurrentPeriod(ReportTaskSchedule schedule, String completedPeriod) {
        if (schedule.getNextRunAt() != null && TaskScheduleTiming.periodKey(schedule, schedule.getNextRunAt()).equals(completedPeriod)) {
            schedule.setNextRunAt(TaskScheduleTiming.nextRun(schedule, schedule.getNextRunAt().plusSeconds(1)));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RunResult generate(Long runId) {
        ReportTaskScheduleRun run = runs.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "定时执行记录不存在"));
        if (!"PROCESSING".equals(run.getStatus())) return result(run);
        ReportTaskSchedule schedule = run.getSchedule();
        if (!schedule.getTemplate().isEnabled()) throw new ApiException(HttpStatus.BAD_REQUEST, "关联模板已停用");

        Set<User> assignees = resolveAssignees(schedule);
        if (assignees.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "没有可填报的有效人员");

        LocalDateTime publishedAt = run.getPublishedAt();
        ReportTask task = new ReportTask();
        task.setName(schedule.getName() + "（" + TaskScheduleTiming.periodKey(schedule, publishedAt) + "）");
        task.setTemplate(schedule.getTemplate()); task.setTemplateVersion(schedule.getTemplateVersion());
        task.setFrequency(schedule.getFrequency()); task.setPeriodLabel(TaskScheduleTiming.periodKey(schedule, publishedAt));
        task.setStartAt(publishedAt); task.setDeadline(publishedAt.plusDays(schedule.getDeadlineDays()));
        task.setAllowLate(schedule.isAllowLate()); task.setStatus("PUBLISHED"); task.setDescription(schedule.getDescription());
        task.setSourceType("SCHEDULED"); task.setSchedule(schedule);
        task.setAssignees(assignees); task.setTargetDepartments(new HashSet<>(schedule.getTargetDepartments()));
        ReportTask saved = tasks.save(task);
        List<ReportTaskDetail> created = new ArrayList<>();
        for (User reporter : assignees) {
            ReportTaskDetail detail = new ReportTaskDetail(); detail.setTask(saved); detail.setReporter(reporter);
            detail.setTemplateVersion(schedule.getTemplateVersion()); created.add(detail);
        }
        details.saveAll(created);
        run.setTask(saved); run.setStatus("SUCCESS"); run.setErrorMessage(null);
        return result(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RunResult fail(Long runId, String message) {
        ReportTaskScheduleRun run = runs.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "定时执行记录不存在"));
        run.setStatus("FAILED"); run.setErrorMessage(trim(message));
        return result(run);
    }

    private Set<User> resolveAssignees(ReportTaskSchedule schedule) {
        Set<User> result = new HashSet<>();
        schedule.getAssignees().stream().filter(access::isEligibleAssignee).forEach(result::add);
        Set<Long> departmentIds = new HashSet<>();
        for (Department root : schedule.getTargetDepartments()) departmentIds.addAll(includingChildren(root));
        if (!departmentIds.isEmpty()) {
            users.findAll().stream().filter(access::isEligibleAssignee)
                    .filter(user -> user.getDepartment() != null && departmentIds.contains(user.getDepartment().getId()))
                    .forEach(result::add);
        }
        return result;
    }

    private Set<Long> includingChildren(Department root) {
        Set<Long> ids = new HashSet<>(); ids.add(root.getId()); boolean changed;
        List<Department> all = departments.findAll();
        do {
            changed = false;
            for (Department department : all) {
                Long parent = department.getParent() == null ? null : department.getParent().getId();
                if (parent != null && ids.contains(parent)) changed |= ids.add(department.getId());
            }
        } while (changed);
        return ids;
    }

    private String trim(String value) {
        if (value == null || value.isBlank()) return "执行失败";
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private RunResult result(ReportTaskScheduleRun run) {
        return new RunResult(run.getId(), run.getPeriodKey(), run.getTask() == null ? null : run.getTask().getId(), run.getStatus(), run.getErrorMessage(), run.getExecutedAt());
    }

    public record RunResult(Long id, String periodKey, Long taskId, String status, String errorMessage, LocalDateTime executedAt) { }

    public record Claim(Long runId, RunResult existingRun, LocalDateTime publishedAt, boolean created, boolean skipped) {
        static Claim created(ReportTaskScheduleRun run, LocalDateTime publishedAt) { return new Claim(run.getId(), null, publishedAt, true, false); }
        static Claim existing(ReportTaskScheduleRun run) { return new Claim(null, new RunResult(run.getId(), run.getPeriodKey(), run.getTask() == null ? null : run.getTask().getId(), run.getStatus(), run.getErrorMessage(), run.getExecutedAt()), null, false, false); }
        static Claim skipped(ReportTaskSchedule schedule, LocalDateTime ignored) { return new Claim(null, null, null, false, true); }
    }
}
