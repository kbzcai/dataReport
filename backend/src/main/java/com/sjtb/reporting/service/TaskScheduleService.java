package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.ReportTaskSchedule;
import com.sjtb.reporting.domain.ReportTaskScheduleRun;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.dto.ScheduleDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRunRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.ReportTemplateVersionRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskScheduleService {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduleService.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String PAUSED = "PAUSED";
    private final ReportTaskScheduleRepository schedules;
    private final ReportTaskScheduleRunRepository runs;
    private final ReportTemplateRepository templates;
    private final ReportTemplateVersionRepository versions;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CurrentUserService current;
    private final AccessControlService access;
    private final TaskScheduleExecutionService executions;

    public TaskScheduleService(ReportTaskScheduleRepository schedules, ReportTaskScheduleRunRepository runs,
                               ReportTemplateRepository templates, ReportTemplateVersionRepository versions, UserRepository users,
                               DepartmentRepository departments, CurrentUserService current, AccessControlService access,
                               TaskScheduleExecutionService executions) {
        this.schedules = schedules; this.runs = runs; this.templates = templates; this.versions = versions;
        this.users = users; this.departments = departments; this.current = current; this.access = access; this.executions = executions;
    }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.Response> list() { assertManager(); return schedules.findAllByOrderByNextRunAtAsc().stream().map(this::response).toList(); }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.RunResponse> runs(Long id) { assertManager(); find(id); return runs.findTop20ByScheduleIdOrderByExecutedAtDesc(id).stream().map(this::runResponse).toList(); }

    @Transactional(readOnly = true)
    public List<ScheduleDtos.Target> targets() {
        assertManager(); List<ScheduleDtos.Target> result = new ArrayList<>();
        departments.findAllByOrderByNameAsc().forEach(item -> result.add(new ScheduleDtos.Target(item.getId(), item.getName(), "DEPARTMENT")));
        users.findAll().stream().filter(access::isEligibleAssignee).sorted(Comparator.comparing(User::getUsername))
                .forEach(item -> result.add(new ScheduleDtos.Target(item.getId(), item.getUsername(), "REPORTER")));
        return result;
    }

    @Transactional
    public ScheduleDtos.Response create(ScheduleDtos.Request request) {
        User user = assertManager(); ReportTaskSchedule schedule = new ReportTaskSchedule(); schedule.setCreatedBy(user); apply(schedule, request);
        return response(schedules.save(schedule));
    }

    @Transactional
    public ScheduleDtos.Response update(Long id, ScheduleDtos.Request request) { assertManager(); ReportTaskSchedule schedule = find(id); apply(schedule, request); return response(schedule); }

    @Transactional
    public ScheduleDtos.Response pause(Long id) { assertManager(); ReportTaskSchedule schedule = find(id); schedule.setStatus(PAUSED); return response(schedule); }

    @Transactional
    public ScheduleDtos.Response resume(Long id) { assertManager(); ReportTaskSchedule schedule = find(id); schedule.setStatus(ACTIVE); schedule.setNextRunAt(TaskScheduleTiming.nextRun(schedule, LocalDateTime.now())); return response(schedule); }

    public ScheduleDtos.RunResponse runNow(Long id) { assertManager(); return execute(id, LocalDateTime.now(), true); }

    @Transactional(readOnly = true)
    public void executeDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = schedules.findByStatusAndNextRunAtLessThanEqual(ACTIVE, now).stream().map(ReportTaskSchedule::getId).toList();
        for (Long id : ids) {
            try { execute(id, now, false); }
            catch (RuntimeException exception) { log.warn("Scheduled task execution failed for schedule {}", id, exception); }
        }
    }

    private ScheduleDtos.RunResponse execute(Long scheduleId, LocalDateTime requestedAt, boolean manual) {
        TaskScheduleExecutionService.Claim claim = executions.claim(scheduleId, requestedAt, manual);
        if (claim.skipped()) return new ScheduleDtos.RunResponse(null, null, null, "SKIPPED", null, LocalDateTime.now());
        if (!claim.created()) return runResponse(claim.existingRun());
        try { return runResponse(executions.generate(claim.runId())); }
        catch (RuntimeException exception) { return runResponse(executions.fail(claim.runId(), exception.getMessage())); }
    }

    private void apply(ReportTaskSchedule schedule, ScheduleDtos.Request request) {
        validateRule(request);
        ReportTemplate template = templates.findById(request.templateId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        ReportTemplateVersion version = versions.findById(request.templateVersionId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template version not found"));
        if (!version.getTemplate().getId().equals(template.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "模板版本不属于所选模板");
        Set<User> assignees = resolveAssignees(request.assigneeIds()); Set<Department> targetDepartments = resolveDepartments(request.departmentIds());
        if (assignees.isEmpty() && resolveDepartmentAssignees(targetDepartments).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "请至少选择一名有效填报人员或目标部门");
        schedule.setName(request.name().trim()); schedule.setTemplate(template); schedule.setTemplateVersion(version);
        schedule.setFrequency(request.frequency().trim().toUpperCase()); schedule.setWeekDay(request.weekDay()); schedule.setDayOfMonth(request.dayOfMonth()); schedule.setMonthOfYear(request.monthOfYear());
        schedule.setPublishTime(request.publishTime()); schedule.setDeadlineDays(request.deadlineDays()); schedule.setAllowLate(request.allowLate());
        schedule.setStartAt(request.startAt()); schedule.setEndAt(request.endAt()); schedule.setDescription(request.description());
        schedule.setAssignees(assignees); schedule.setTargetDepartments(targetDepartments);
        if (schedule.getStatus() == null) schedule.setStatus(ACTIVE);
        schedule.setNextRunAt(TaskScheduleTiming.nextRun(schedule, LocalDateTime.now()));
    }

    private void validateRule(ScheduleDtos.Request request) {
        String frequency = request.frequency() == null ? "" : request.frequency().toUpperCase();
        if (!Set.of("WEEKLY", "MONTHLY", "YEARLY").contains(frequency)) throw new ApiException(HttpStatus.BAD_REQUEST, "频率仅支持 WEEKLY、MONTHLY、YEARLY");
        if (request.startAt() != null && request.endAt() != null && request.startAt().isAfter(request.endAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "结束时间不能早于开始时间");
        if ("WEEKLY".equals(frequency) && (request.weekDay() == null || request.weekDay() < 1 || request.weekDay() > 7)) throw new ApiException(HttpStatus.BAD_REQUEST, "每周任务须选择 1-7 的星期");
        if ("MONTHLY".equals(frequency) && (request.dayOfMonth() == null || request.dayOfMonth() < 0 || request.dayOfMonth() > 31)) throw new ApiException(HttpStatus.BAD_REQUEST, "每月任务日期应为 1-31，0 表示最后一天");
        if ("YEARLY".equals(frequency) && (request.monthOfYear() == null || request.monthOfYear() < 1 || request.monthOfYear() > 12 || request.dayOfMonth() == null || request.dayOfMonth() < 0 || request.dayOfMonth() > 31)) throw new ApiException(HttpStatus.BAD_REQUEST, "每年任务须选择月份和日期");
    }

    private User assertManager() {
        User user = current.current();
        if (access.isAdmin(user) || user.getRoles().contains(Role.MAINTAINER)) return user;
        throw new ApiException(HttpStatus.FORBIDDEN, "仅系统管理员或模板管理员可管理定时发布规则");
    }

    private ReportTaskSchedule find(Long id) { return schedules.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "定时发布规则不存在")); }

    private Set<User> resolveAssignees(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>(); Set<Long> unique = new HashSet<>(ids); if (unique.contains(null)) throw new ApiException(HttpStatus.BAD_REQUEST, "填报人员不能为空");
        List<User> found = users.findAllById(unique);
        if (found.size() != unique.size() || found.stream().anyMatch(user -> !access.isEligibleAssignee(user))) throw new ApiException(HttpStatus.BAD_REQUEST, "填报人员必须是启用且具有 REPORT_EDIT 的 REPORTER");
        return new HashSet<>(found);
    }

    private Set<Department> resolveDepartments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>(); Set<Long> unique = new HashSet<>(ids); if (unique.contains(null)) throw new ApiException(HttpStatus.BAD_REQUEST, "目标部门不能为空");
        List<Department> found = departments.findAllById(unique);
        if (found.size() != unique.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "存在无效部门"); return new HashSet<>(found);
    }

    private Set<User> resolveDepartmentAssignees(Set<Department> roots) {
        Set<Long> ids = new HashSet<>(); List<Department> all = departments.findAll();
        for (Department root : roots) {
            ids.add(root.getId()); boolean changed;
            do { changed = false; for (Department department : all) { Long parent = department.getParent() == null ? null : department.getParent().getId(); if (parent != null && ids.contains(parent)) changed |= ids.add(department.getId()); } } while (changed);
        }
        Set<User> result = new HashSet<>();
        if (!ids.isEmpty()) users.findAll().stream().filter(access::isEligibleAssignee).filter(user -> user.getDepartment() != null && ids.contains(user.getDepartment().getId())).forEach(result::add);
        return result;
    }

    private ScheduleDtos.Response response(ReportTaskSchedule item) {
        return new ScheduleDtos.Response(item.getId(), item.getName(), item.getTemplate().getId(), item.getTemplate().getName(), item.getTemplateVersion().getId(), item.getTemplateVersion().getVersionNo(), item.getFrequency(), item.getWeekDay(), item.getDayOfMonth(), item.getMonthOfYear(), item.getPublishTime(), item.getDeadlineDays(), item.isAllowLate(), item.getStatus(), item.getStartAt(), item.getEndAt(), item.getNextRunAt(), item.getDescription(), item.getAssignees().stream().map(User::getId).toList(), item.getTargetDepartments().stream().map(Department::getId).toList(), item.getCreatedBy().getUsername(), item.getCreatedAt(), item.getUpdatedAt());
    }

    private ScheduleDtos.RunResponse runResponse(ReportTaskScheduleRun item) { return new ScheduleDtos.RunResponse(item.getId(), item.getPeriodKey(), item.getTask() == null ? null : item.getTask().getId(), item.getStatus(), item.getErrorMessage(), item.getExecutedAt()); }
    private ScheduleDtos.RunResponse runResponse(TaskScheduleExecutionService.RunResult item) { return new ScheduleDtos.RunResponse(item.id(), item.periodKey(), item.taskId(), item.status(), item.errorMessage(), item.executedAt()); }
}
