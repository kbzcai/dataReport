package com.sjtb.reporting.dto;
import com.sjtb.reporting.domain.LateFillRequestStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
public final class LateFillDtos { private LateFillDtos(){} public record Create(@NotNull Long taskId,@NotNull Long leaderId,@NotBlank @Size(max=500) String reason){} public record Review(@NotNull @Future LocalDateTime lateDeadline,@Size(max=500) String reviewComment){} public record Leader(Long id,String username){} public record Response(Long id,Long taskId,String taskName,Long requesterId,String requesterName,Long leaderId,String leaderName,String reason,LateFillRequestStatus status,LocalDateTime lateDeadline,String reviewComment,LocalDateTime createdAt,LocalDateTime reviewedAt){} }
