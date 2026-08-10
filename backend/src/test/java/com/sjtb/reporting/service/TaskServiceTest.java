package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.Permission;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.dto.TaskDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportTaskDetailRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class TaskServiceTest {
    @Test
    void reporterReceivesOnlyOwnDetailProgressAndKeepsExistingTaskProgress() {
        Department department = department(1L);
        User reporter = user(7L, "reporter", department, Set.of(Role.REPORTER));
        ReportTask task = task(3L, reporter, department);
        ReportRecordRepository records = mock(ReportRecordRepository.class);
        when(records.findTaskProgressByTaskIds(anyList(), eq(Set.of(com.sjtb.reporting.domain.ReportStatus.SUBMITTED, com.sjtb.reporting.domain.ReportStatus.APPROVED))))
                .thenReturn(List.of(new TaskDtos.ProgressAggregation(3L, 1)));
        when(records.findTaskDetailProgressByTaskIdsAndReporterId(anyList(), eq(7L), anySet()))
                .thenReturn(List.of(new TaskDtos.DetailProgressAggregation(3L, 4, 1, 1, 1, 1, LocalDateTime.of(2026, 8, 10, 9, 30))));

        TaskService service = service(reporter, records, List.of(task), List.of(department));

        TaskDtos.Response response = service.list().get(0);

        assertThat(response.progress()).isEqualTo(new TaskDtos.Progress(1, 1, 0));
        assertThat(response.detailProgress()).isEqualTo(new TaskDtos.DetailProgress("SELF", 4, 1, 1, 1, 1, LocalDateTime.of(2026, 8, 10, 9, 30)));
        verify(records).findTaskDetailProgressByTaskIdsAndReporterId(anyList(), eq(7L), anySet());
        verify(records, never()).findTaskDetailProgressByTaskIds(anyList(), anySet());
    }

    @Test
    void leaderReceivesTaskScopeDetailProgressForManagedTasks() {
        Department department = department(1L);
        User leader = user(9L, "leader", department, Set.of(Role.LEADER));
        User reporter = user(7L, "reporter", department, Set.of(Role.REPORTER));
        ReportTask task = task(3L, reporter, department);
        ReportRecordRepository records = mock(ReportRecordRepository.class);
        when(records.findTaskProgressByTaskIds(anyList(), anySet())).thenReturn(List.of());
        when(records.findTaskDetailProgressByTaskIds(anyList(), anySet()))
                .thenReturn(List.of(new TaskDtos.DetailProgressAggregation(3L, 6, 2, 2, 1, 1, LocalDateTime.of(2026, 8, 10, 10, 0))));

        TaskService service = service(leader, records, List.of(task), List.of(department));

        TaskDtos.Response response = service.list().get(0);

        assertThat(response.detailProgress()).isEqualTo(new TaskDtos.DetailProgress("TASK", 6, 2, 2, 1, 1, LocalDateTime.of(2026, 8, 10, 10, 0)));
        verify(records).findTaskDetailProgressByTaskIds(anyList(), anySet());
        verify(records, never()).findTaskDetailProgressByTaskIdsAndReporterId(anyList(), eq(9L), anySet());
    }

    @Test
    void administratorReceivesTaskScopeAndEmptyProgressWhenNoRecordsExist() {
        Department department = department(1L);
        User admin = user(1L, "admin", null, Set.of(Role.ADMIN));
        User reporter = user(7L, "reporter", department, Set.of(Role.REPORTER));
        ReportTask task = task(3L, reporter, department);
        ReportRecordRepository records = mock(ReportRecordRepository.class);
        when(records.findTaskProgressByTaskIds(anyList(), anySet())).thenReturn(List.of());
        when(records.findTaskDetailProgressByTaskIds(anyList(), anySet())).thenReturn(List.of());

        TaskService service = service(admin, records, List.of(task), List.of(department));

        TaskDtos.Response response = service.list().get(0);

        assertThat(response.detailProgress()).isEqualTo(new TaskDtos.DetailProgress("TASK", 0, 0, 0, 0, 0, null));
        assertThat(response.progress()).isEqualTo(new TaskDtos.Progress(1, 0, 1));
    }

    @Test
    void detailProgressRejectsUnassignedReporterAndReturnsNotFoundForUnknownTask() {
        Department department = department(1L);
        User currentReporter = user(8L, "current", department, Set.of(Role.REPORTER));
        User assignedReporter = user(7L, "assigned", department, Set.of(Role.REPORTER));
        ReportTask task = task(3L, assignedReporter, department);
        ReportTaskRepository tasks = mock(ReportTaskRepository.class);
        when(tasks.findById(3L)).thenReturn(Optional.of(task));
        when(tasks.findById(404L)).thenReturn(Optional.empty());
        TaskService service = service(currentReporter, mock(ReportRecordRepository.class), tasks, List.of(department));

        assertThatThrownBy(() -> service.detailProgress(3L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.detailProgress(404L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void leaderCannotModifyOrDeleteScheduledTask() {
        Department department = department(1L);
        User leader = user(9L, "leader", department, Set.of(Role.LEADER));
        User reporter = user(7L, "reporter", department, Set.of(Role.REPORTER));
        ReportTask task = task(3L, reporter, department);
        task.setSourceType("SCHEDULED");
        ReportTaskRepository tasks = mock(ReportTaskRepository.class);
        when(tasks.findById(3L)).thenReturn(Optional.of(task));
        TaskService service = service(leader, mock(ReportRecordRepository.class), tasks, List.of(department));

        assertThatThrownBy(() -> service.update(3L, null)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static TaskService service(User currentUser, ReportRecordRepository records, List<ReportTask> listedTasks, List<Department> departments) {
        ReportTaskRepository tasks = mock(ReportTaskRepository.class);
        when(tasks.findByAssigneesIdOrderByDeadlineAsc(currentUser.getId())).thenReturn(listedTasks);
        when(tasks.findAllByOrderByDeadlineAsc()).thenReturn(listedTasks);
        return service(currentUser, records, tasks, departments);
    }

    private static TaskService service(User currentUser, ReportRecordRepository records, ReportTaskRepository tasks, List<Department> departmentList) {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(departments.findAll()).thenReturn(departmentList);
        CurrentUserService current = mock(CurrentUserService.class);
        when(current.current()).thenReturn(currentUser);
        return new TaskService(tasks, mock(ReportTaskDetailRepository.class), mock(ReportTemplateRepository.class), mock(UserRepository.class), departments,
                records, current, mock(TemplateService.class), new AccessControlService(departments));
    }

    private static ReportTask task(Long id, User assignee, Department department) {
        ReportTemplate template = new ReportTemplate();
        ReflectionTestUtils.setField(template, "id", 2L);
        template.setName("Template");
        ReportTemplateVersion version = new ReportTemplateVersion();
        ReflectionTestUtils.setField(version, "id", 4L);
        version.setTemplate(template);
        version.setVersionNo(1);
        ReportTask task = new ReportTask();
        ReflectionTestUtils.setField(task, "id", id);
        task.setName("Task");
        task.setTemplate(template);
        task.setTemplateVersion(version);
        task.setFrequency("MONTHLY");
        task.setStatus("PUBLISHED");
        task.setAssignees(Set.of(assignee));
        task.setTargetDepartments(Set.of(department));
        return task;
    }

    private static Department department(Long id) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setName("Department");
        return department;
    }

    private static User user(Long id, String username, Department department, Set<Role> roles) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setDepartment(department);
        user.setRoles(roles);
        user.setPermissions(Set.of(Permission.REPORT_EDIT));
        return user;
    }
}
