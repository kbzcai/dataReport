package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_change_log", indexes = {@Index(name = "idx_change_log_record", columnList = "report_id,created_at"), @Index(name = "idx_change_log_actor", columnList = "actor_id,created_at")})
public class ReportChangeLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "report_id", nullable = false) private Long reportId;
    @Column(name = "template_id", nullable = false) private Long templateId;
    @Column(name = "actor_id", nullable = false) private Long actorId;
    @Column(nullable = false, length = 64) private String actorName;
    @Column(nullable = false, length = 32) private String action;
    @Lob @Column(columnDefinition = "TEXT") private String beforeDataJson;
    @Lob @Column(columnDefinition = "TEXT") private String afterDataJson;
    @Column(length = 500) private String reason;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; }
    public Long getReportId() { return reportId; }
    public Long getTemplateId() { return templateId; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public String getBeforeDataJson() { return beforeDataJson; }
    public String getAfterDataJson() { return afterDataJson; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setReportId(Long value) { reportId = value; }
    public void setTemplateId(Long value) { templateId = value; }
    public void setActorId(Long value) { actorId = value; }
    public void setActorName(String value) { actorName = value; }
    public void setAction(String value) { action = value; }
    public void setBeforeDataJson(String value) { beforeDataJson = value; }
    public void setAfterDataJson(String value) { afterDataJson = value; }
    public void setReason(String value) { reason = value; }
}
