package com.sjtb.reporting.repository;
import com.sjtb.reporting.domain.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReportLateFillRequestRepository extends JpaRepository<ReportLateFillRequest, Long> {
    boolean existsByTaskIdAndRequesterIdAndStatus(Long taskId, Long requesterId, LateFillRequestStatus status);
    Optional<ReportLateFillRequest> findFirstByTaskIdAndRequesterIdAndStatusAndLateDeadlineAfterOrderByLateDeadlineDesc(Long taskId, Long requesterId, LateFillRequestStatus status, LocalDateTime now);
    List<ReportLateFillRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<ReportLateFillRequest> findByLeaderIdOrderByCreatedAtDesc(Long leaderId);
}
