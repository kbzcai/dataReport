package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportImportError;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportImportErrorRepository extends JpaRepository<ReportImportError, Long> {
    List<ReportImportError> findByBatchIdOrderByIdAsc(Long batchId);
}
