package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportImportBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportImportBatchRepository extends JpaRepository<ReportImportBatch, Long> {
    List<ReportImportBatch> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
    List<ReportImportBatch> findAllByOrderByCreatedAtDesc();
}
