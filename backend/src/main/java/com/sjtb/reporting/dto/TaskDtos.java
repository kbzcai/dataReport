package com.sjtb.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import com.sjtb.reporting.domain.Role;

public final class TaskDtos {
    private TaskDtos() { }
    public record Request(@NotBlank String name, @NotNull Long templateId, Long templateVersionId, @NotBlank String frequency, String periodLabel,
                          LocalDateTime startAt, LocalDateTime deadline, boolean allowLate, @NotBlank String status, String description, List<Long> assigneeIds, List<Long> departmentIds) { }
    public record Response(Long id, String name, Long templateId, String templateName, Long templateVersionId, int templateVersionNo, String frequency, String periodLabel,
                           LocalDateTime startAt, LocalDateTime deadline, boolean allowLate, String status, String description, List<Long> assigneeIds,
                           List<Assignee> assignees, List<Long> departmentIds, Progress progress) { }
    public record Assignee(Long id, String username, List<Role> roles) { }
    public record AssignableTarget(Long id, String name, Long parentId, List<Assignee> users) { }
    public record Progress(long assigneeCount, long submittedAssigneeCount, long pendingAssigneeCount) { }
    public record ProgressAggregation(Long taskId, long submittedAssigneeCount) { }
    public record Reminder(Long taskId, String taskName, String templateName, String periodLabel, LocalDateTime deadline, String level) { }
    public record Overview(long total, long published, long dueSoon, long overdue, long completed, long pendingAssignees) { }
}
