package com.sjtb.reporting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleDtos {
    private ScheduleDtos() { }
    public record Request(@NotBlank String name, @NotNull Long templateId, @NotNull Long templateVersionId, @NotBlank String frequency,
                          Integer weekDay, Integer dayOfMonth, Integer monthOfYear, @NotNull LocalTime publishTime,
                          @Min(1) @Max(365) int deadlineDays, boolean allowLate, LocalDateTime startAt, LocalDateTime endAt,
                          String description, List<Long> assigneeIds, List<Long> departmentIds) { }
    public record Response(Long id, String name, Long templateId, String templateName, Long templateVersionId, int templateVersionNo,
                           String frequency, Integer weekDay, Integer dayOfMonth, Integer monthOfYear, LocalTime publishTime,
                           int deadlineDays, boolean allowLate, String status, LocalDateTime startAt, LocalDateTime endAt,
                           LocalDateTime nextRunAt, String description, List<Long> assigneeIds, List<Long> departmentIds,
                           String createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record RunResponse(Long id, String periodKey, Long taskId, String status, String errorMessage, LocalDateTime executedAt) { }
    public record Target(Long id, String name, String type) { }
}
