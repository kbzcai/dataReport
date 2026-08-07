package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportRecordValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRecordValueRepository extends JpaRepository<ReportRecordValue, Long> {
    void deleteByRecordId(Long recordId);
    List<ReportRecordValue> findByFieldKeyAndValueText(String fieldKey, String valueText);
    List<ReportRecordValue> findByFieldKeyAndValueTextAndRecord_Template_Id(String fieldKey, String valueText, Long templateId);
}
