package com.sjtb.reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.domain.*;
import com.sjtb.reporting.dto.ChangeRequestDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportChangeRequestRepository;
import com.sjtb.reporting.repository.ReportRecordRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ChangeRequestService {
    private final ReportChangeRequestRepository requests; private final ReportRecordRepository records; private final ReportService reports; private final TemplateService templates; private final ReportAuditService audit; private final CurrentUserService current; private final ObjectMapper mapper; private final AccessControlService access;
    public ChangeRequestService(ReportChangeRequestRepository requests, ReportRecordRepository records, ReportService reports, TemplateService templates, ReportAuditService audit, CurrentUserService current, ObjectMapper mapper, AccessControlService access) { this.requests = requests; this.records = records; this.reports = reports; this.templates = templates; this.audit = audit; this.current = current; this.mapper = mapper; this.access = access; }
    public ChangeRequestDtos.Response create(Long reportId, ChangeRequestDtos.CreateRequest input) {
        User user = current.current(); access.requireEdit(user); ReportRecord report = lockedReport(reportId);
        if (!report.getReporter().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the reporter may request a change");
        if (report.getStatus() == ReportStatus.DRAFT) throw new ApiException(HttpStatus.BAD_REQUEST, "Draft records can be edited directly");
        if (requests.existsByReportIdAndStatus(reportId, ChangeRequestStatus.PENDING)) throw new ApiException(HttpStatus.CONFLICT, "A pending change request already exists for this record");
        if (!sameVersion(report.getUpdatedAt(), input.baseUpdatedAt())) throw new ApiException(HttpStatus.CONFLICT, "The record has changed; refresh it before requesting a change");
        reports.validateData(templateVersion(report), input.data());
        ReportChangeRequest entity = new ReportChangeRequest(); entity.setReport(report); entity.setRequester(user); entity.setProposedDataJson(write(input.data())); entity.setReason(input.reason().trim()); entity.setBaseUpdatedAt(input.baseUpdatedAt());
        return response(requests.save(entity));
    }
    @Transactional(readOnly = true) public List<ChangeRequestDtos.Response> list(ChangeRequestStatus status) {
        User user = current.current(); access.requireView(user); java.util.Set<Long> scope = access.scopeDepartmentIds(user);
        List<ReportChangeRequest> source = status == null ? requests.findAll() : requests.findByStatusOrderByCreatedAtAsc(status);
        source = source.stream().filter(item -> item.getRequester().getId().equals(user.getId()) || access.canReadRecord(user, item.getReport(), scope)).toList();
        return source.stream().map(this::response).toList();
    }
    public ChangeRequestDtos.Response approve(Long id, ChangeRequestDtos.ReviewRequest input) { return review(id, input, ChangeRequestStatus.APPROVED); }
    public ChangeRequestDtos.Response reject(Long id, ChangeRequestDtos.ReviewRequest input) { if (input == null || input.reviewComment() == null || input.reviewComment().isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection comment is required"); return review(id, input, ChangeRequestStatus.REJECTED); }
    public ChangeRequestDtos.Response cancel(Long id) {
        ReportChangeRequest entity = find(id); User user = current.current();
        if (!entity.getRequester().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the requester may cancel this request");
        if (entity.getStatus() != ChangeRequestStatus.PENDING) throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be cancelled");
        entity.setStatus(ChangeRequestStatus.CANCELLED); return response(entity);
    }
    private ChangeRequestDtos.Response review(Long id, ChangeRequestDtos.ReviewRequest input, ChangeRequestStatus target) {
        User reviewer = current.current(); access.requireEdit(reviewer);
        ReportChangeRequest entity = find(id); if (!(access.isAdmin(reviewer) || (access.isLeader(reviewer) && access.canManageDepartment(reviewer, entity.getReport().getReporter().getDepartment())))) throw new ApiException(HttpStatus.FORBIDDEN, "Only a scoped leader may review requests"); if (entity.getStatus() != ChangeRequestStatus.PENDING) throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be reviewed");
        if (target == ChangeRequestStatus.APPROVED) {
            ReportRecord report = lockedReport(entity.getReport().getId());
            if (!sameVersion(report.getUpdatedAt(), entity.getBaseUpdatedAt())) throw new ApiException(HttpStatus.CONFLICT, "The original record changed after this request was submitted");
            Map<String, Object> data = read(entity.getProposedDataJson());
            reports.applyApprovedChange(report, data, entity.getReason());
        }
        entity.setStatus(target); entity.setReviewer(reviewer); entity.setReviewComment(input == null ? null : input.reviewComment()); entity.setReviewedAt(LocalDateTime.now()); return response(entity);
    }
    private ReportChangeRequest find(Long id) { return requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Change request not found")); }
    private ReportRecord lockedReport(Long id) { return records.findByIdForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report record not found")); }
    private ReportTemplateVersion templateVersion(ReportRecord report) { return report.getTemplateVersion() == null ? templates.currentVersion(report.getTemplate()) : report.getTemplateVersion(); }
    private boolean sameVersion(LocalDateTime left, LocalDateTime right) { return left != null && right != null && left.withNano(left.getNano() / 1_000_000 * 1_000_000).equals(right.withNano(right.getNano() / 1_000_000 * 1_000_000)); }
    private String write(Map<String, Object> data) { try { return mapper.writeValueAsString(data); } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Change data cannot be serialized"); } }
    private Map<String, Object> read(String dataJson) { try { return mapper.readValue(dataJson, new TypeReference<>() {}); } catch (Exception e) { throw new IllegalStateException("Stored change request data is corrupt", e); } }
    private ChangeRequestDtos.Response response(ReportChangeRequest entity) { ReportRecord report = entity.getReport(); return new ChangeRequestDtos.Response(entity.getId(), report.getId(), report.getTask() == null ? null : report.getTask().getId(), report.getTask() == null ? null : report.getTask().getName(), report.getTemplate().getId(), report.getTemplate().getName(), entity.getRequester().getId(), entity.getRequester().getUsername(), read(entity.getProposedDataJson()), entity.getReason(), entity.getBaseUpdatedAt(), entity.getStatus(), entity.getReviewer() == null ? null : entity.getReviewer().getUsername(), entity.getReviewComment(), entity.getCreatedAt(), entity.getReviewedAt()); }
}
