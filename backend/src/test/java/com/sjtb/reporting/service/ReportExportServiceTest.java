package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class ReportExportServiceTest {
    @Test
    void rejectsTaskThatDoesNotBelongToRequestedTemplate() {
        ReportService reports = mock(ReportService.class);
        TemplateService templates = mock(TemplateService.class);
        TaskService tasks = mock(TaskService.class);
        when(reports.list(1L)).thenReturn(List.of());
        ReportTemplate other = new ReportTemplate();
        ReflectionTestUtils.setField(other, "id", 2L);
        ReportTask task = new ReportTask();
        task.setTemplate(other);
        when(tasks.find(99L)).thenReturn(task);
        when(templates.get(1L)).thenReturn(new TemplateDtos.Response(1L, "template", "Template", null, List.of(), true, "PUBLISHED", 2L, 1));

        ReportExportService service = new ReportExportService(reports, templates, tasks);

        assertThatThrownBy(() -> service.export(1L, 99L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("does not belong to template");
    }
}
