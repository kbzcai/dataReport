package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    boolean existsByCode(String code);
    Optional<ReportTemplate> findByCode(String code);
}
