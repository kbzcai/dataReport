package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.Permission;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTaskSchedule;
import com.sjtb.reporting.domain.ReportTaskScheduleRun;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRepository;
import com.sjtb.reporting.repository.ReportTaskScheduleRunRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.ReportTemplateVersionRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class TaskScheduleServiceTest {
    @Test
    void leaderAndReporterCannotManageScheduleRules() {
        assertForbidden(new User(), Set.of(Role.LEADER));
        assertForbidden(new User(), Set.of(Role.REPORTER));
    }

    @Test
    void generatingTaskExpandsSelectedDepartmentToEligibleChildReporters() {
        DepartmentRepository departments = mock(DepartmentRepository.class);
        Department root = department(1L, null); Department child = department(2L, root);
        User reporter = new User(); ReflectionTestUtils.setField(reporter, "id", 8L);
        reporter.setEnabled(true); reporter.setRoles(Set.of(Role.REPORTER)); reporter.setPermissions(Set.of(Permission.REPORT_EDIT)); reporter.setDepartment(child);
        ReportTemplate template = new ReportTemplate(); template.setEnabled(true);
        ReportTemplateVersion version = new ReportTemplateVersion(); version.setTemplate(template); version.setVersionNo(1);
        ReportTaskSchedule schedule = new ReportTaskSchedule(); schedule.setName("月度填报"); schedule.setTemplate(template); schedule.setTemplateVersion(version);
        schedule.setFrequency("MONTHLY"); schedule.setDeadlineDays(7); schedule.setPublishTime(LocalTime.of(9, 0)); schedule.setTargetDepartments(Set.of(root));
        ReportTaskScheduleRun run = new ReportTaskScheduleRun(); ReflectionTestUtils.setField(run, "id", 3L);
        run.setSchedule(schedule); run.setPeriodKey("2026-08"); run.setPublishedAt(LocalDateTime.of(2026, 8, 1, 9, 0)); run.setStatus("PROCESSING");
        ReportTaskScheduleRunRepository runs = mock(ReportTaskScheduleRunRepository.class);
        when(runs.findById(3L)).thenReturn(Optional.of(run));
        UserRepository users = mock(UserRepository.class); when(users.findAll()).thenReturn(List.of(reporter));
        when(departments.findAll()).thenReturn(List.of(root, child));
        ReportTaskRepository tasks = mock(ReportTaskRepository.class); when(tasks.save(org.mockito.ArgumentMatchers.any(ReportTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        com.sjtb.reporting.repository.ReportTaskDetailRepository details = mock(com.sjtb.reporting.repository.ReportTaskDetailRepository.class);
        TaskScheduleExecutionService service = new TaskScheduleExecutionService(mock(ReportTaskScheduleRepository.class), runs, tasks, details, users, departments, new AccessControlService(departments));

        TaskScheduleExecutionService.RunResult result = service.generate(3L);

        org.mockito.ArgumentCaptor<ReportTask> captured = org.mockito.ArgumentCaptor.forClass(ReportTask.class);
        verify(tasks).save(captured.capture());
        assertThat(captured.getValue().getAssignees()).containsExactly(reporter);
        assertThat(captured.getValue().isScheduled()).isTrue();
        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(details).saveAll(org.mockito.ArgumentMatchers.argThat(items -> ((List<?>) items).size() == 1));
    }

    @Test
    void manualRunRetriesFailedRunInsteadOfCreatingDuplicateTask() {
        ReportTaskSchedule schedule = new ReportTaskSchedule(); ReflectionTestUtils.setField(schedule, "id", 4L);
        schedule.setFrequency("MONTHLY"); schedule.setPublishTime(LocalTime.of(9, 0)); schedule.setDayOfMonth(1);
        ReportTaskScheduleRun failed = new ReportTaskScheduleRun(); ReflectionTestUtils.setField(failed, "id", 5L);
        failed.setSchedule(schedule); failed.setPeriodKey("2026-08"); failed.setPublishedAt(LocalDateTime.of(2026, 8, 1, 9, 0)); failed.setStatus("FAILED");
        ReportTaskScheduleRepository schedules = mock(ReportTaskScheduleRepository.class);
        when(schedules.findWithLockById(4L)).thenReturn(Optional.of(schedule));
        ReportTaskScheduleRunRepository runs = mock(ReportTaskScheduleRunRepository.class);
        when(runs.findByScheduleIdAndPeriodKey(4L, "2026-08")).thenReturn(Optional.of(failed));
        TaskScheduleExecutionService service = new TaskScheduleExecutionService(schedules, runs, mock(ReportTaskRepository.class),
                mock(com.sjtb.reporting.repository.ReportTaskDetailRepository.class), mock(UserRepository.class), mock(DepartmentRepository.class), mock(AccessControlService.class));

        TaskScheduleExecutionService.Claim claim = service.claim(4L, LocalDateTime.of(2026, 8, 10, 10, 0), true);

        assertThat(claim.created()).isTrue();
        assertThat(claim.runId()).isEqualTo(5L);
        assertThat(failed.getStatus()).isEqualTo("PROCESSING");
    }

    private static void assertForbidden(User user, Set<Role> roles) {
        user.setRoles(roles);
        CurrentUserService current = mock(CurrentUserService.class);
        when(current.current()).thenReturn(user);
        AccessControlService access = mock(AccessControlService.class);
        TaskScheduleService service = new TaskScheduleService(mock(ReportTaskScheduleRepository.class), mock(ReportTaskScheduleRunRepository.class),
                mock(ReportTemplateRepository.class), mock(ReportTemplateVersionRepository.class), mock(UserRepository.class), mock(DepartmentRepository.class),
                current, access, mock(TaskScheduleExecutionService.class));

        assertThatThrownBy(service::list).isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static Department department(Long id, Department parent) {
        Department department = new Department(); ReflectionTestUtils.setField(department, "id", id); department.setName("D" + id); department.setParent(parent); return department;
    }
}
