package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Searchable shadow value for a dynamic JSON field. */
@Entity
@Table(name = "report_record_value",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_record_value_field", columnNames = {"record_id", "field_key"}),
                @UniqueConstraint(name = "uk_template_field_unique_value", columnNames = {"template_id", "field_key", "unique_marker", "value_hash"})
        },
        indexes = {
                @Index(name = "idx_value_field_text", columnList = "field_key"),
                @Index(name = "idx_value_field_number", columnList = "field_key,value_number"),
                @Index(name = "idx_value_record", columnList = "record_id")
        })
public class ReportRecordValue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "record_id") private ReportRecord record;
    @Column(name = "template_id") private Long templateId;
    @Column(name = "field_key", nullable = false, length = 64) private String fieldKey;
    @Column(name = "unique_marker", length = 1) private String uniqueMarker;
    @Column(name = "value_hash", length = 64) private String valueHash;
    @Column(name = "value_text", length = 2000) private String valueText;
    @Column(name = "value_number", precision = 30, scale = 8) private BigDecimal valueNumber;
    @Column(name = "value_date") private LocalDate valueDate;
    public Long getId() { return id; }
    public ReportRecord getRecord() { return record; }
    public void setRecord(ReportRecord value) { record = value; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long value) { templateId = value; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String value) { fieldKey = value; }
    public String getUniqueMarker() { return uniqueMarker; }
    public void setUniqueMarker(String value) { uniqueMarker = value; }
    public String getValueHash() { return valueHash; }
    public void setValueHash(String value) { valueHash = value; }
    public String getValueText() { return valueText; }
    public void setValueText(String value) { valueText = value; }
    public BigDecimal getValueNumber() { return valueNumber; }
    public void setValueNumber(BigDecimal value) { valueNumber = value; }
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate value) { valueDate = value; }
}
