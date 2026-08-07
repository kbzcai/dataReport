package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ChangeRequestStatus;
import com.sjtb.reporting.domain.ReportChangeRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportChangeRequestRepository extends JpaRepository<ReportChangeRequest, Long> {
    boolean existsByReportId(Long reportId);
    void deleteByReportId(Long reportId);
    boolean existsByReportIdAndStatus(Long reportId, ChangeRequestStatus status);
    List<ReportChangeRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<ReportChangeRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(Long requesterId, ChangeRequestStatus status);
    List<ReportChangeRequest> findByStatusOrderByCreatedAtAsc(ChangeRequestStatus status);
}
