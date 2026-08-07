package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_import_batch", indexes = {@Index(name = "idx_import_batch_creator", columnList = "creator_id,created_at"), @Index(name = "idx_import_batch_status", columnList = "status")})
public class ReportImportBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 255) private String originalFileName;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "creator_id") private User creator;
    @Column(nullable = false, length = 20) private String status = "PROCESSING";
    @Column(nullable = false) private int importedRows;
    @Column(nullable = false) private int failedRows;
    @Column(length = 1000) private String summary;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    public Long getId() { return id; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String value) { originalFileName = value; }
    public User getCreator() { return creator; }
    public void setCreator(User value) { creator = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getImportedRows() { return importedRows; }
    public void setImportedRows(int value) { importedRows = value; }
    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int value) { failedRows = value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
}
