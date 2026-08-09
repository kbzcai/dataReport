package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.exception.ApiException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ReportServiceTest {
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

    private static ReportTemplate template(Long id) {
        ReportTemplate template = new ReportTemplate();
        ReflectionTestUtils.setField(template, "id", id);
        return template;
    }
}
