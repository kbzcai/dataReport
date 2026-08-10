package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_task_schedule_run", uniqueConstraints = @UniqueConstraint(name = "uk_schedule_run_period", columnNames = {"schedule_id", "period_key"}), indexes = @Index(name = "idx_schedule_run_schedule_time", columnList = "schedule_id,executed_at"))
public class ReportTaskScheduleRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "schedule_id") private ReportTaskSchedule schedule;
    @Column(name = "period_key", nullable = false, length = 32) private String periodKey;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "task_id") private ReportTask task;
    @Column(nullable = false, length = 16) private String status;
    @Column(length = 1000) private String errorMessage;
    @Column(nullable = false) private LocalDateTime publishedAt;
    @Column(nullable = false) private LocalDateTime executedAt = LocalDateTime.now();
    public Long getId() { return id; } public ReportTaskSchedule getSchedule() { return schedule; } public void setSchedule(ReportTaskSchedule value) { schedule = value; }
    public String getPeriodKey() { return periodKey; } public void setPeriodKey(String value) { periodKey = value; }
    public ReportTask getTask() { return task; } public void setTask(ReportTask value) { task = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String value) { errorMessage = value; }
    public LocalDateTime getPublishedAt() { return publishedAt; } public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
    public void setExecutedAt(LocalDateTime value) { executedAt = value; }
    public LocalDateTime getExecutedAt() { return executedAt; }
}
