package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_change_request", indexes = {@Index(name = "idx_change_request_status", columnList = "status"), @Index(name = "idx_change_request_report", columnList = "report_id")})
public class ReportChangeRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "report_id") private ReportRecord report;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "requester_id") private User requester;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String proposedDataJson;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false) private LocalDateTime baseUpdatedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ChangeRequestStatus status = ChangeRequestStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id") private User reviewer;
    @Column(length = 500) private String reviewComment;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    public Long getId() { return id; } public ReportRecord getReport() { return report; } public void setReport(ReportRecord value) { report = value; }
    public User getRequester() { return requester; } public void setRequester(User value) { requester = value; }
    public String getProposedDataJson() { return proposedDataJson; } public void setProposedDataJson(String value) { proposedDataJson = value; }
    public String getReason() { return reason; } public void setReason(String value) { reason = value; }
    public LocalDateTime getBaseUpdatedAt() { return baseUpdatedAt; } public void setBaseUpdatedAt(LocalDateTime value) { baseUpdatedAt = value; }
    public ChangeRequestStatus getStatus() { return status; } public void setStatus(ChangeRequestStatus value) { status = value; }
    public User getReviewer() { return reviewer; } public void setReviewer(User value) { reviewer = value; }
    public String getReviewComment() { return reviewComment; } public void setReviewComment(String value) { reviewComment = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
}
