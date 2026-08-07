package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTaskDetail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTaskDetailRepository extends JpaRepository<ReportTaskDetail, Long> {
    List<ReportTaskDetail> findByTaskId(Long taskId);
    Optional<ReportTaskDetail> findByTaskIdAndReporterId(Long taskId, Long reporterId);
    void deleteByTaskId(Long taskId);
}
