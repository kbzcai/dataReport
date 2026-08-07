package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Per-reporter task instance. It keeps assignment and completion state independent. */
@Entity
@Table(name = "report_task_detail",
        uniqueConstraints = @UniqueConstraint(name = "uk_task_detail_reporter", columnNames = {"task_id", "reporter_id"}),
        indexes = {@Index(name = "idx_task_detail_task_status", columnList = "task_id,status"), @Index(name = "idx_task_detail_reporter", columnList = "reporter_id,status")})
public class ReportTaskDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "task_id") private ReportTask task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "reporter_id") private User reporter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_version_id") private ReportTemplateVersion templateVersion;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    private LocalDateTime submittedAt;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public ReportTask getTask() { return task; }
    public void setTask(ReportTask value) { task = value; }
    public User getReporter() { return reporter; }
    public void setReporter(User value) { reporter = value; }
    public ReportTemplateVersion getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(ReportTemplateVersion value) { templateVersion = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
}
