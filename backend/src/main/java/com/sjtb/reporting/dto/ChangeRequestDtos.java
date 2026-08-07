package com.sjtb.reporting.dto;

import com.sjtb.reporting.domain.ChangeRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public final class ChangeRequestDtos {
    private ChangeRequestDtos() { }
    public record CreateRequest(@NotNull Map<String, Object> data, @NotBlank String reason, @NotNull LocalDateTime baseUpdatedAt) { }
    public record ReviewRequest(String reviewComment) { }
    public record Response(Long id, Long reportId, Long taskId, String taskName, Long templateId, String templateName, Long requesterId, String requesterName,
                           Map<String, Object> proposedData, String reason, LocalDateTime baseUpdatedAt, ChangeRequestStatus status,
                           String reviewerName, String reviewComment, LocalDateTime createdAt, LocalDateTime reviewedAt) { }
}
