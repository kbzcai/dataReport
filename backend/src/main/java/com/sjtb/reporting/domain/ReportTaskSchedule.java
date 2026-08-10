package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "report_task_schedule", indexes = {
        @Index(name = "idx_task_schedule_status_next", columnList = "status,next_run_at"),
        @Index(name = "idx_task_schedule_template", columnList = "template_id")
})
public class ReportTaskSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 128) private String name;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "template_id") private ReportTemplate template;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "template_version_id") private ReportTemplateVersion templateVersion;
    @Column(nullable = false, length = 16) private String frequency;
    private Integer weekDay;
    private Integer dayOfMonth;
    private Integer monthOfYear;
    @Column(nullable = false) private LocalTime publishTime;
    @Column(nullable = false) private int deadlineDays;
    @Column(nullable = false) private boolean allowLate;
    @Column(nullable = false, length = 16) private String status = "ACTIVE";
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @Column(nullable = false) private LocalDateTime nextRunAt;
    @Column(length = 500) private String description;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
    @ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "report_task_schedule_assignee", joinColumns = @JoinColumn(name = "schedule_id"), inverseJoinColumns = @JoinColumn(name = "user_id")) private Set<User> assignees = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "report_task_schedule_department", joinColumns = @JoinColumn(name = "schedule_id"), inverseJoinColumns = @JoinColumn(name = "department_id")) private Set<Department> targetDepartments = new HashSet<>();
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public String getName() { return name; } public void setName(String value) { name = value; }
    public ReportTemplate getTemplate() { return template; } public void setTemplate(ReportTemplate value) { template = value; }
    public ReportTemplateVersion getTemplateVersion() { return templateVersion; } public void setTemplateVersion(ReportTemplateVersion value) { templateVersion = value; }
    public String getFrequency() { return frequency; } public void setFrequency(String value) { frequency = value; }
    public Integer getWeekDay() { return weekDay; } public void setWeekDay(Integer value) { weekDay = value; }
    public Integer getDayOfMonth() { return dayOfMonth; } public void setDayOfMonth(Integer value) { dayOfMonth = value; }
    public Integer getMonthOfYear() { return monthOfYear; } public void setMonthOfYear(Integer value) { monthOfYear = value; }
    public LocalTime getPublishTime() { return publishTime; } public void setPublishTime(LocalTime value) { publishTime = value; }
    public int getDeadlineDays() { return deadlineDays; } public void setDeadlineDays(int value) { deadlineDays = value; }
    public boolean isAllowLate() { return allowLate; } public void setAllowLate(boolean value) { allowLate = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public LocalDateTime getStartAt() { return startAt; } public void setStartAt(LocalDateTime value) { startAt = value; }
    public LocalDateTime getEndAt() { return endAt; } public void setEndAt(LocalDateTime value) { endAt = value; }
    public LocalDateTime getNextRunAt() { return nextRunAt; } public void setNextRunAt(LocalDateTime value) { nextRunAt = value; }
    public String getDescription() { return description; } public void setDescription(String value) { description = value; }
    public User getCreatedBy() { return createdBy; } public void setCreatedBy(User value) { createdBy = value; }
    public Set<User> getAssignees() { return assignees; } public void setAssignees(Set<User> value) { assignees = value; }
    public Set<Department> getTargetDepartments() { return targetDepartments; } public void setTargetDepartments(Set<Department> value) { targetDepartments = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getUpdatedAt() { return updatedAt; }
}
