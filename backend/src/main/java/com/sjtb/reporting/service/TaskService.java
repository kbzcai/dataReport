package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportStatus;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.ReportTaskDetail;
import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.dto.TaskDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTaskDetailRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.UserRepository;
import com.sjtb.reporting.repository.DepartmentRepository;
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
    private static final String DRAFT = "DRAFT"; private static final String PUBLISHED = "PUBLISHED"; private static final String CLOSED = "CLOSED";
    private final ReportTaskRepository tasks; private final ReportTaskDetailRepository details; private final ReportTemplateRepository templates; private final UserRepository users; private final DepartmentRepository departments; private final ReportRecordRepository records; private final CurrentUserService current; private final TemplateService templateService; private final AccessControlService access;
    public TaskService(ReportTaskRepository tasks, ReportTaskDetailRepository details, ReportTemplateRepository templates, UserRepository users, DepartmentRepository departments, ReportRecordRepository records, CurrentUserService current, TemplateService templateService, AccessControlService access) { this.tasks = tasks; this.details = details; this.templates = templates; this.users = users; this.departments = departments; this.records = records; this.current = current; this.templateService = templateService; this.access = access; }
    @Transactional(readOnly = true) public List<TaskDtos.Response> list() {
        User user = current.current();
        Set<Long> scope = access.isLeader(user) && !access.isAdmin(user) ? scopeDepartmentIds(user) : Set.of();
        List<ReportTask> visible = access.isAdmin(user) ? tasks.findAllByOrderByDeadlineAsc()
                : access.isLeader(user) ? tasks.findAllByOrderByDeadlineAsc().stream().filter(task -> canManage(task, user, scope)).toList()
                : tasks.findByAssigneesIdOrderByDeadlineAsc(user.getId());
        return responses(visible);
    }
    @Transactional(readOnly = true) public List<TaskDtos.AssignableTarget> assignableTargets() {
        User publisher = current.current(); Set<Long> scope = scopeDepartmentIds(publisher);
        List<Department> all = departments.findAllByOrderByNameAsc();
        return all.stream().filter(department -> scope.isEmpty() || scope.contains(department.getId())).map(department -> {
            List<TaskDtos.Assignee> members = users.findAll().stream().filter(User::isEnabled).filter(user -> user.getDepartment() != null && department.getId().equals(user.getDepartment().getId())).filter(this::isAssignable).map(this::assignee).sorted(Comparator.comparing(TaskDtos.Assignee::username)).toList();
            return new TaskDtos.AssignableTarget(department.getId(), department.getName(), department.getParent() == null ? null : department.getParent().getId(), members);
        }).toList();
    }
    @Transactional(readOnly = true) public List<TaskDtos.Reminder> reminders() {
        User user = current.current(); LocalDateTime now = LocalDateTime.now();
        return tasks.findByAssigneesIdOrderByDeadlineAsc(user.getId()).stream().filter(task -> PUBLISHED.equals(task.getStatus()))
                .filter(task -> !records.existsByTaskIdAndReporterIdAndStatusIn(task.getId(), user.getId(), Set.of(ReportStatus.SUBMITTED, ReportStatus.APPROVED)))
                .map(task -> new TaskDtos.Reminder(task.getId(), task.getName(), task.getTemplate().getName(), task.getPeriodLabel(), task.getDeadline(), reminderLevel(task, now))).toList();
    }
    @Transactional(readOnly = true) public TaskDtos.Overview overview() {
        User user = current.current();
        Set<Long> scope = access.isLeader(user) && !access.isAdmin(user) ? scopeDepartmentIds(user) : Set.of();
        List<ReportTask> visible = access.isAdmin(user) ? tasks.findAllByOrderByDeadlineAsc()
                : tasks.findAllByOrderByDeadlineAsc().stream().filter(task -> canManage(task, user, scope)).toList();
        LocalDateTime now = LocalDateTime.now(); List<TaskDtos.Response> all = responses(visible);
        long published = all.stream().filter(task -> PUBLISHED.equals(task.status())).count();
        long overdue = all.stream().filter(task -> PUBLISHED.equals(task.status()) && task.deadline() != null && task.deadline().isBefore(now)).count();
        long dueSoon = all.stream().filter(task -> PUBLISHED.equals(task.status()) && task.deadline() != null && !task.deadline().isBefore(now) && !task.deadline().isAfter(now.plusDays(3))).count();
        long completed = all.stream().filter(task -> task.progress().assigneeCount() > 0 && task.progress().pendingAssigneeCount() == 0).count();
        return new TaskDtos.Overview(all.size(), published, dueSoon, overdue, completed, all.stream().mapToLong(task -> task.progress().pendingAssigneeCount()).sum());
    }
    public TaskDtos.Response create(TaskDtos.Request request) { return save(new ReportTask(), request); }
    public TaskDtos.Response update(Long id, TaskDtos.Request request) { ReportTask task = find(id); assertCanManage(task, current.current()); return save(task, request); }
    public void delete(Long id) {
        ReportTask task = find(id); assertCanManage(task, current.current());
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
        User publisher = current.current();
        if (!access.isAdmin(publisher) && !access.isLeader(publisher)) throw new ApiException(HttpStatus.FORBIDDEN, "Only a leader may publish tasks");
        Set<Department> targetDepartments = resolveDepartments(request.departmentIds(), publisher);
        task.setTargetDepartments(targetDepartments);
        Set<User> assignees = resolveAssignees(request.assigneeIds(), publisher);
        Set<Long> targetIds = targetDepartments.stream().flatMap(department -> departmentIdsIncludingChildren(department).stream()).collect(java.util.stream.Collectors.toSet());
        if (!targetIds.isEmpty()) users.findAll().stream().filter(User::isEnabled).filter(this::isAssignable).filter(user -> user.getDepartment() != null && targetIds.contains(user.getDepartment().getId())).forEach(assignees::add);
        if (assignees.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Tasks must resolve to at least one enabled REPORTER with REPORT_EDIT permission");
        task.setAssignees(assignees);
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
        return new TaskDtos.Response(task.getId(), task.getName(), task.getTemplate().getId(), task.getTemplate().getName(), version.getId(), version.getVersionNo(), task.getFrequency(), task.getPeriodLabel(), task.getStartAt(), task.getDeadline(), task.isAllowLate(), task.getStatus(), task.getDescription(), assignees.stream().map(TaskDtos.Assignee::id).toList(), assignees, task.getTargetDepartments().stream().map(Department::getId).toList(), new TaskDtos.Progress(assigneeCount, submittedCount, Math.max(0, assigneeCount - submittedCount)));
    }

    private TaskDtos.Assignee assignee(User user) { return new TaskDtos.Assignee(user.getId(), user.getUsername(), user.getRoles().stream().sorted().toList()); }

    private Set<User> resolveAssignees(List<Long> requestedIds, User publisher) {
        if (requestedIds == null || requestedIds.isEmpty()) return new HashSet<>();
        Set<Long> ids = new HashSet<>(requestedIds);
        if (ids.contains(null)) throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee ids cannot contain null");
        List<User> found = users.findAllById(ids);
        if (found.size() != ids.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "One or more assignees do not exist");
        Set<Long> scope = scopeDepartmentIds(publisher);
        for (User user : found) if (!user.isEnabled() || !isAssignable(user) || (!scope.isEmpty() && (user.getDepartment() == null || !scope.contains(user.getDepartment().getId())))) throw new ApiException(HttpStatus.BAD_REQUEST, "Assignees must be enabled REPORTER users with REPORT_EDIT permission in your department scope");
        return new HashSet<>(found);
    }
    private boolean isAssignable(User user) { return access.isEligibleAssignee(user); }
    private Set<Department> resolveDepartments(List<Long> requestedIds, User publisher) {
        if (requestedIds == null || requestedIds.isEmpty()) return new HashSet<>();
        Set<Long> scope = scopeDepartmentIds(publisher); List<Department> found = departments.findAllById(new HashSet<>(requestedIds));
        if (found.size() != new HashSet<>(requestedIds).size() || (!scope.isEmpty() && found.stream().anyMatch(department -> !scope.contains(department.getId()))) ) throw new ApiException(HttpStatus.BAD_REQUEST, "Departments must be within your department scope");
        return new HashSet<>(found);
    }
    private Set<Long> scopeDepartmentIds(User publisher) {
        if (access.isAdmin(publisher)) return Set.of();
        Set<Long> scope = access.scopeDepartmentIds(publisher);
        if (scope.isEmpty()) throw new ApiException(HttpStatus.FORBIDDEN, "A data leader must belong to a department");
        return scope;
    }
    private Set<Long> departmentIdsIncludingChildren(Department root) {
        Set<Long> ids = new HashSet<>(); ids.add(root.getId()); boolean changed;
        do { changed = false; for (Department department : departments.findAll()) { Long parent = department.getParent() == null ? null : department.getParent().getId(); if (parent != null && ids.contains(parent)) changed |= ids.add(department.getId()); } } while (changed);
        return ids;
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

    private void assertCanManage(ReportTask task, User user) { if (!canManage(task, user)) throw new ApiException(HttpStatus.FORBIDDEN, "Task is outside your department scope"); }
    public boolean canManage(ReportTask task, User user) {
        return canManage(task, user, scopeDepartmentIds(user));
    }
    private boolean canManage(ReportTask task, User user, Set<Long> scope) {
        if (access.isAdmin(user)) return true;
        if (!access.isLeader(user)) return false;
        if (task.getTargetDepartments().isEmpty() && task.getAssignees().isEmpty()) return false;
        return task.getTargetDepartments().stream().allMatch(item -> scope.contains(item.getId()))
                && task.getAssignees().stream().allMatch(item -> item.getDepartment() != null && scope.contains(item.getDepartment().getId()));
    }
    private String reminderLevel(ReportTask task, LocalDateTime now) { if (task.getDeadline() != null && task.getDeadline().isBefore(now)) return "OVERDUE"; if (task.getDeadline() != null && !task.getDeadline().isAfter(now.plusDays(3))) return "DUE_SOON"; return "PENDING"; }
}
