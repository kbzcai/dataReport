package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportChangeLogRepository extends JpaRepository<ReportChangeLog, Long> {
    List<ReportChangeLog> findByReportIdOrderByCreatedAtDesc(Long reportId);
}
