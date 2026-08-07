package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable snapshot of a template field definition used by tasks and records. */
@Entity
@Table(name = "report_template_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_template_version_no", columnNames = {"template_id", "version_no"}),
        indexes = @Index(name = "idx_template_version_template", columnList = "template_id,version_no"))
public class ReportTemplateVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "template_id") private ReportTemplate template;
    @Column(name = "version_no", nullable = false) private int versionNo;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String columnsJson;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; }
    public ReportTemplate getTemplate() { return template; }
    public void setTemplate(ReportTemplate template) { this.template = template; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public String getColumnsJson() { return columnsJson; }
    public void setColumnsJson(String columnsJson) { this.columnsJson = columnsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
