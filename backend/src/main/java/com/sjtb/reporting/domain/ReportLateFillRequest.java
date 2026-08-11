package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_late_fill_request", indexes = {@Index(name = "idx_late_fill_request_task_user", columnList = "task_id,requester_id"), @Index(name = "idx_late_fill_request_status", columnList = "status")})
public class ReportLateFillRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "task_id") private ReportTask task;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "requester_id") private User requester;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "leader_id") private User leader;
    @Column(nullable = false, length = 500) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private LateFillRequestStatus status = LateFillRequestStatus.PENDING;
    private LocalDateTime lateDeadline; private LocalDateTime reviewedAt;
    @Column(length = 500) private String reviewComment;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId(){return id;} public ReportTask getTask(){return task;} public void setTask(ReportTask v){task=v;} public User getRequester(){return requester;} public void setRequester(User v){requester=v;} public User getLeader(){return leader;} public void setLeader(User v){leader=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public LateFillRequestStatus getStatus(){return status;} public void setStatus(LateFillRequestStatus v){status=v;} public LocalDateTime getLateDeadline(){return lateDeadline;} public void setLateDeadline(LocalDateTime v){lateDeadline=v;} public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;} public String getReviewComment(){return reviewComment;} public void setReviewComment(String v){reviewComment=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}
