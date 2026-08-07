package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTemplateVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTemplateVersionRepository extends JpaRepository<ReportTemplateVersion, Long> {
    Optional<ReportTemplateVersion> findTopByTemplateIdOrderByVersionNoDesc(Long templateId);
    List<ReportTemplateVersion> findByTemplateIdOrderByVersionNoDesc(Long templateId);
    int countByTemplateId(Long templateId);
}
