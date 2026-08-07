package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportStatus;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.ReportTaskDetail;
import com.sjtb.reporting.dto.TaskDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTaskDetailRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class TaskService {
    private static final Set<Role> ASSIGNABLE_ROLES = Set.of(Role.REPORTER, Role.EDITOR);
    private static final String DRAFT = "DRAFT"; private static final String PUBLISHED = "PUBLISHED"; private static final String CLOSED = "CLOSED";
    private final ReportTaskRepository tasks; private final ReportTaskDetailRepository details; private final ReportTemplateRepository templates; private final UserRepository users; private final ReportRecordRepository records; private final CurrentUserService current; private final TemplateService templateService;
    public TaskService(ReportTaskRepository tasks, ReportTaskDetailRepository details, ReportTemplateRepository templates, UserRepository users, ReportRecordRepository records, CurrentUserService current, TemplateService templateService) { this.tasks = tasks; this.details = details; this.templates = templates; this.users = users; this.records = records; this.current = current; this.templateService = templateService; }
    @Transactional(readOnly = true) public List<TaskDtos.Response> list() {
        User user = current.current(); boolean manager = isManager(user);
        List<ReportTask> visible = manager ? tasks.findAllByOrderByDeadlineAsc() : tasks.findByAssigneesIdOrderByDeadlineAsc(user.getId());
        return responses(visible);
    }
    @Transactional(readOnly = true) public List<TaskDtos.Assignee> assignableUsers() { return users.findEnabledUsersWithAnyRole(ASSIGNABLE_ROLES).stream().map(this::assignee).toList(); }
    @Transactional(readOnly = true) public List<TaskDtos.Reminder> reminders() {
        User user = current.current(); LocalDateTime now = LocalDateTime.now();
        return tasks.findByAssigneesIdOrderByDeadlineAsc(user.getId()).stream().filter(task -> PUBLISHED.equals(task.getStatus()))
                .filter(task -> !records.existsByTaskIdAndReporterIdAndStatusIn(task.getId(), user.getId(), Set.of(ReportStatus.SUBMITTED, ReportStatus.APPROVED)))
                .map(task -> new TaskDtos.Reminder(task.getId(), task.getName(), task.getTemplate().getName(), task.getPeriodLabel(), task.getDeadline(), reminderLevel(task, now))).toList();
    }
    @Transactional(readOnly = true) public TaskDtos.Overview overview() {
        LocalDateTime now = LocalDateTime.now(); List<TaskDtos.Response> all = responses(tasks.findAllByOrderByDeadlineAsc());
        long published = all.stream().filter(task -> PUBLISHED.equals(task.status())).count();
        long overdue = all.stream().filter(task -> PUBLISHED.equals(task.status()) && task.deadline() != null && task.deadline().isBefore(now)).count();
        long dueSoon = all.stream().filter(task -> PUBLISHED.equals(task.status()) && task.deadline() != null && !task.deadline().isBefore(now) && !task.deadline().isAfter(now.plusDays(3))).count();
        long completed = all.stream().filter(task -> task.progress().assigneeCount() > 0 && task.progress().pendingAssigneeCount() == 0).count();
        return new TaskDtos.Overview(all.size(), published, dueSoon, overdue, completed, all.stream().mapToLong(task -> task.progress().pendingAssigneeCount()).sum());
    }
    public TaskDtos.Response create(TaskDtos.Request request) { return save(new ReportTask(), request); }
    public TaskDtos.Response update(Long id, TaskDtos.Request request) { return save(find(id), request); }
    public void delete(Long id) {
        ReportTask task = find(id);
        if (records.existsByTaskId(id)) throw new ApiException(HttpStatus.CONFLICT, "Task with submitted records cannot be deleted");
        tasks.delete(task);
    }
    public ReportTask find(Long id) { return tasks.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found")); }
    private TaskDtos.Response save(ReportTask task, TaskDtos.Request request) {
        validateSchedule(request.startAt(), request.deadline());
        boolean existing = task.getId() != null;
        if (existing) validateUpdate(task, request);
        else if (!DRAFT.equals(request.status())) throw new ApiException(HttpStatus.BAD_REQUEST, "New tasks must start in DRAFT status");

        task.setName(request.name().trim());
        ReportTemplate template = templates.findById(request.templateId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found"));
        ReportTemplateVersion version = request.templateVersionId() == null ? templateService.currentVersion(template) : templateService.findVersion(request.templateVersionId());
        if (!version.getTemplate().getId().equals(template.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Template version does not belong to template");
        if (existing && records.existsByTaskId(task.getId()) && task.getTemplateVersion() != null && !task.getTemplateVersion().getId().equals(version.getId())) throw new ApiException(HttpStatus.CONFLICT, "Task template version cannot change after records exist");
        task.setTemplate(template); task.setTemplateVersion(version);
        task.setFrequency(request.frequency().trim()); task.setPeriodLabel(request.periodLabel()); task.setStartAt(request.startAt()); task.setDeadline(request.deadline()); task.setAllowLate(request.allowLate()); task.setStatus(request.status()); task.setDescription(request.description());
        task.setAssignees(resolveAssignees(request.assigneeIds()));
        ReportTask saved = tasks.save(task); syncDetails(saved);
        return responses(List.of(saved)).get(0);
    }

    private List<TaskDtos.Response> responses(List<ReportTask> source) {
        if (source.isEmpty()) return List.of();
        Map<Long, TaskDtos.ProgressAggregation> progress = records.findTaskProgressByTaskIds(source.stream().map(ReportTask::getId).toList(), Set.of(ReportStatus.SUBMITTED, ReportStatus.APPROVED)).stream().collect(java.util.stream.Collectors.toMap(TaskDtos.ProgressAggregation::taskId, Function.identity()));
        return source.stream().map(task -> response(task, progress.get(task.getId()))).toList();
    }

    private TaskDtos.Response response(ReportTask task, TaskDtos.ProgressAggregation aggregation) {
        List<TaskDtos.Assignee> assignees = task.getAssignees().stream().sorted(Comparator.comparing(User::getUsername)).map(this::assignee).toList();
        long assigneeCount = assignees.size();
        long submittedCount = aggregation == null ? 0 : aggregation.submittedAssigneeCount();
        ReportTemplateVersion version = task.getTemplateVersion() == null ? templateService.currentVersion(task.getTemplate()) : task.getTemplateVersion();
        return new TaskDtos.Response(task.getId(), task.getName(), task.getTemplate().getId(), task.getTemplate().getName(), version.getId(), version.getVersionNo(), task.getFrequency(), task.getPeriodLabel(), task.getStartAt(), task.getDeadline(), task.isAllowLate(), task.getStatus(), task.getDescription(), assignees.stream().map(TaskDtos.Assignee::id).toList(), assignees, new TaskDtos.Progress(assigneeCount, submittedCount, Math.max(0, assigneeCount - submittedCount)));
    }

    private TaskDtos.Assignee assignee(User user) { return new TaskDtos.Assignee(user.getId(), user.getUsername(), user.getRoles().stream().sorted().toList()); }

    private Set<User> resolveAssignees(List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return new HashSet<>();
        Set<Long> ids = new HashSet<>(requestedIds);
        if (ids.contains(null)) throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee ids cannot contain null");
        List<User> found = users.findAllById(ids);
        if (found.size() != ids.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "One or more assignees do not exist");
        for (User user : found) if (!user.isEnabled() || user.getRoles().stream().noneMatch(ASSIGNABLE_ROLES::contains)) throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee '" + user.getUsername() + "' must be an enabled REPORTER or EDITOR");
        return new HashSet<>(found);
    }
    public void markSubmitted(ReportTask task, User reporter) {
        if (task == null) return;
        ReportTaskDetail detail = details.findByTaskIdAndReporterId(task.getId(), reporter.getId()).orElseGet(() -> {
            ReportTaskDetail created = new ReportTaskDetail(); created.setTask(task); created.setReporter(reporter); created.setTemplateVersion(task.getTemplateVersion()); return created;
        });
        detail.setStatus("SUBMITTED"); detail.setSubmittedAt(LocalDateTime.now()); details.save(detail);
    }
    private void syncDetails(ReportTask task) {
        if (task.getAssignees().isEmpty()) return;
        Set<Long> existing = details.findByTaskId(task.getId()).stream().map(item -> item.getReporter().getId()).collect(java.util.stream.Collectors.toSet());
        List<ReportTaskDetail> created = new java.util.ArrayList<>();
        for (User assignee : task.getAssignees()) if (!existing.contains(assignee.getId())) {
            ReportTaskDetail detail = new ReportTaskDetail(); detail.setTask(task); detail.setReporter(assignee); detail.setTemplateVersion(task.getTemplateVersion()); created.add(detail);
        }
        if (!created.isEmpty()) details.saveAll(created);
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime deadline) {
        if (startAt != null && deadline != null && startAt.isAfter(deadline)) throw new ApiException(HttpStatus.BAD_REQUEST, "Task startAt must not be after deadline");
    }

    private void validateUpdate(ReportTask task, TaskDtos.Request request) {
        if (records.existsByTaskId(task.getId()) && !task.getTemplate().getId().equals(request.templateId())) throw new ApiException(HttpStatus.CONFLICT, "Task template cannot change after records exist");
        if (CLOSED.equals(task.getStatus())) throw new ApiException(HttpStatus.BAD_REQUEST, "Closed tasks cannot be modified");
        String requested = request.status();
        if (task.getStatus().equals(requested)) return;
        boolean validTransition = (DRAFT.equals(task.getStatus()) && PUBLISHED.equals(requested)) || (PUBLISHED.equals(task.getStatus()) && CLOSED.equals(requested));
        if (!validTransition) throw new ApiException(HttpStatus.BAD_REQUEST, "Task status must follow DRAFT -> PUBLISHED -> CLOSED");
    }

    private boolean isManager(User user) { return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER); }
    private String reminderLevel(ReportTask task, LocalDateTime now) { if (task.getDeadline() != null && task.getDeadline().isBefore(now)) return "OVERDUE"; if (task.getDeadline() != null && !task.getDeadline().isAfter(now.plusDays(3))) return "DUE_SOON"; return "PENDING"; }
}
