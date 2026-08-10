package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "report_task", indexes = @Index(name = "idx_task_template_status", columnList = "template_id,status"))
public class ReportTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 128) private String name;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "template_id") private ReportTemplate template;
    /** Version selected when the task is created. Nullable for legacy tasks; service resolves the latest version. */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_version_id") private ReportTemplateVersion templateVersion;
    @Column(nullable = false, length = 20) private String frequency;
    @Column(length = 64) private String periodLabel;
    private LocalDateTime startAt;
    private LocalDateTime deadline;
    @Column(nullable = false) private boolean allowLate = false;
    @Column(nullable = false, length = 20) private String status = "DRAFT";
    @Column(length = 500) private String description;
    @Column(nullable = false, length = 16) private String sourceType = "MANUAL";
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "schedule_id") private ReportTaskSchedule schedule;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "report_task_assignee", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignees = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "report_task_department", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> targetDepartments = new HashSet<>();
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public String getName() { return name; } public void setName(String value) { name = value; }
    public ReportTemplate getTemplate() { return template; } public void setTemplate(ReportTemplate value) { template = value; }
    public ReportTemplateVersion getTemplateVersion() { return templateVersion; } public void setTemplateVersion(ReportTemplateVersion value) { templateVersion = value; }
    public String getFrequency() { return frequency; } public void setFrequency(String value) { frequency = value; }
    public String getPeriodLabel() { return periodLabel; } public void setPeriodLabel(String value) { periodLabel = value; }
    public LocalDateTime getStartAt() { return startAt; } public void setStartAt(LocalDateTime value) { startAt = value; }
    public LocalDateTime getDeadline() { return deadline; } public void setDeadline(LocalDateTime value) { deadline = value; }
    public boolean isAllowLate() { return allowLate; } public void setAllowLate(boolean value) { allowLate = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public String getSourceType() { return sourceType; } public void setSourceType(String value) { sourceType = value; }
    public ReportTaskSchedule getSchedule() { return schedule; } public void setSchedule(ReportTaskSchedule value) { schedule = value; }
    public boolean isScheduled() { return "SCHEDULED".equals(sourceType); }
    public Set<User> getAssignees() { return assignees; } public void setAssignees(Set<User> value) { assignees = value; }
    public Set<Department> getTargetDepartments() { return targetDepartments; } public void setTargetDepartments(Set<Department> value) { targetDepartments = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
