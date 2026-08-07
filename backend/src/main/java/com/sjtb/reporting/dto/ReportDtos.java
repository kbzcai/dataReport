package com.sjtb.reporting.dto;

import com.sjtb.reporting.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() { }
    public record Request(@NotNull Long templateId, Long taskId, Long templateVersionId, @NotNull Map<String, Object> data, ReportStatus status) {
        public Request(Long templateId, Long taskId, Map<String, Object> data, ReportStatus status) { this(templateId, taskId, null, data, status); }
        public Request(Long templateId, Map<String, Object> data, ReportStatus status) { this(templateId, null, null, data, status); }
    }
    public record ReviewRequest(@NotNull ReportStatus status, String reviewComment) { }
    public record Response(Long id, Long templateId, String templateName, Long templateVersionId, int templateVersionNo, Long taskId, String taskName, Long reporterId, String reporterName,
                           Map<String, Object> data, ReportStatus status, String reviewComment,
                           LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record Summary(Long templateId, String templateName, Long reporterId, String reporterName, long recordCount) { }
    public record PageResponse(List<Response> records, long total, int page, int size, int totalPages) { }
}
