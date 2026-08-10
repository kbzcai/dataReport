package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_record", indexes = {
        @Index(name = "idx_record_template_reporter", columnList = "template_id,reporter_id"),
        @Index(name = "idx_record_task_status", columnList = "task_id,status"),
        @Index(name = "idx_record_task_reporter_status", columnList = "task_id,reporter_id,status")
})
public class ReportRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "template_id") private ReportTemplate template;
    /** Snapshot version used to validate and render this record. Nullable for records created before versioning. */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_version_id") private ReportTemplateVersion templateVersion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id") private ReportTask task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "reporter_id") private User reporter;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String dataJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportStatus status = ReportStatus.DRAFT;
    @Column(length = 500) private String reviewComment;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public ReportTemplate getTemplate() { return template; } public void setTemplate(ReportTemplate template) { this.template = template; }
    public ReportTemplateVersion getTemplateVersion() { return templateVersion; } public void setTemplateVersion(ReportTemplateVersion templateVersion) { this.templateVersion = templateVersion; }
    public ReportTask getTask() { return task; } public void setTask(ReportTask task) { this.task = task; }
    public User getReporter() { return reporter; } public void setReporter(User reporter) { this.reporter = reporter; }
    public String getDataJson() { return dataJson; } public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public ReportStatus getStatus() { return status; } public void setStatus(ReportStatus status) { this.status = status; }
    public String getReviewComment() { return reviewComment; } public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
