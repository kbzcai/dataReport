package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.Permission;
import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.ReportStatus;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.dto.ReportDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportChangeRequestRepository;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportRecordValueRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ReportServiceTest {
    @Test
    void reporterCannotFillWithoutPublishedTask() {
        ReportTemplate template = template(1L);
        User reporter = new User();
        ReflectionTestUtils.setField(reporter, "id", 7L);
        reporter.setRoles(Set.of(Role.REPORTER));

        ReportService service = new ReportService(
                null, null, null, null, null, null, null, new ObjectMapper(), new AccessControlService(null));

        assertThatThrownBy(() -> service.validateTask(null, template, reporter))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("task is required");
    }

    @Test
    void reporterCannotFillPastDeadlineEvenWhenTaskAllowsLateSubmission() {
        ReportTemplate template = template(1L);
        ReportTask task = new ReportTask();
        task.setTemplate(template);
        task.setStatus("PUBLISHED");
        task.setDeadline(LocalDateTime.now().minusMinutes(1));
        task.setAllowLate(true);
        User reporter = new User();
        ReflectionTestUtils.setField(reporter, "id", 7L);
        reporter.setRoles(Set.of(Role.REPORTER));
        task.setAssignees(Set.of(reporter));

        ReportService service = new ReportService(
                null, null, null, null, null, null, null, new ObjectMapper(), new AccessControlService(null));

        assertThatThrownBy(() -> service.validateTask(task, template, reporter))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("past its deadline");
    }

    @Test
    void publishedLegacyTaskWithoutAssigneesCannotBeFilledByNonAdmin() {
        ReportTemplate template = template(1L);
        ReportTask task = new ReportTask();
        task.setTemplate(template);
        task.setStatus("PUBLISHED");
        User reporter = new User();
        ReflectionTestUtils.setField(reporter, "id", 7L);
        reporter.setRoles(Set.of(Role.REPORTER));

        ReportService service = new ReportService(
                null, null, null, null, null, null, null, new ObjectMapper(), new AccessControlService(null));

        assertThatThrownBy(() -> service.validateTask(task, template, reporter))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("at least one assignee");
    }

    @Test
    void scopedLeaderWithReportEditCanUpdateLateUnassignedSubmittedReport() {
        Department department = department(1L);
        User leader = user(9L, "leader", department, Set.of(Role.LEADER), Set.of(Permission.REPORT_EDIT));
        User reporter = user(7L, "reporter", department, Set.of(Role.REPORTER), Set.of(Permission.REPORT_EDIT));
        ReportTemplate template = template(1L);
        template.setName("Test template");
        ReportTemplateVersion version = version(2L, template);
        ReportTask task = new ReportTask();
        ReflectionTestUtils.setField(task, "id", 3L);
        task.setName("Expired task");
        task.setTemplate(template);
        task.setTemplateVersion(version);
        task.setStatus("PUBLISHED");
        task.setDeadline(LocalDateTime.now().minusDays(1));
        task.setAssignees(Set.of(reporter));
        ReportRecord record = new ReportRecord();
        ReflectionTestUtils.setField(record, "id", 4L);
        record.setTemplate(template);
        record.setTemplateVersion(version);
        record.setTask(task);
        record.setReporter(reporter);
        record.setStatus(ReportStatus.SUBMITTED);
        record.setDataJson("{}");

        ReportRecordRepository records = mock(ReportRecordRepository.class);
        ReportRecordValueRepository values = mock(ReportRecordValueRepository.class);
        TemplateService templates = mock(TemplateService.class);
        CurrentUserService currentUsers = mock(CurrentUserService.class);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(records.findById(4L)).thenReturn(Optional.of(record));
        when(records.save(record)).thenReturn(record);
        when(templates.find(1L)).thenReturn(template);
        when(templates.findVersion(2L)).thenReturn(version);
        when(templates.columns(version)).thenReturn(List.of());
        when(currentUsers.current()).thenReturn(leader);
        when(departments.findAll()).thenReturn(List.of());

        ReportService service = new ReportService(records, values, mock(ReportChangeRequestRepository.class), templates,
                mock(TaskService.class), mock(ReportAuditService.class), currentUsers, new ObjectMapper(), new AccessControlService(departments));

        assertThatCode(() -> service.update(4L, new ReportDtos.Request(1L, null, 2L, Map.of(), null)))
                .doesNotThrowAnyException();
        verify(records).save(record);
    }

    private static ReportTemplate template(Long id) {
        ReportTemplate template = new ReportTemplate();
        ReflectionTestUtils.setField(template, "id", id);
        return template;
    }

    private static ReportTemplateVersion version(Long id, ReportTemplate template) {
        ReportTemplateVersion version = new ReportTemplateVersion();
        ReflectionTestUtils.setField(version, "id", id);
        version.setTemplate(template);
        version.setVersionNo(1);
        return version;
    }

    private static Department department(Long id) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private static User user(Long id, String username, Department department, Set<Role> roles, Set<Permission> permissions) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        user.setDepartment(department);
        user.setRoles(roles);
        user.setPermissions(permissions);
        return user;
    }
}
