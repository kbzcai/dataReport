package com.sjtb.reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.domain.*;
import com.sjtb.reporting.dto.ReportDtos;
import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportRecordValueRepository;
import com.sjtb.reporting.repository.ReportChangeRequestRepository;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

@Service @Transactional
public class ReportService {
    private final ReportRecordRepository records; private final ReportRecordValueRepository values; private final ReportChangeRequestRepository changeRequests; private final TemplateService templates; private final TaskService tasks; private final ReportAuditService audit; private final CurrentUserService currentUsers; private final ObjectMapper mapper;
    public ReportService(ReportRecordRepository records, ReportRecordValueRepository values, ReportChangeRequestRepository changeRequests, TemplateService templates, TaskService tasks, ReportAuditService audit, CurrentUserService currentUsers, ObjectMapper mapper) { this.records = records; this.values = values; this.changeRequests = changeRequests; this.templates = templates; this.tasks = tasks; this.audit = audit; this.currentUsers = currentUsers; this.mapper = mapper; }
    @Transactional(readOnly = true) public List<ReportDtos.Response> list(Long templateId) {
        User user = currentUsers.current(); boolean leadership = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        List<ReportRecord> source = leadership ? (templateId == null ? records.findAll() : records.findByTemplateIdOrderByUpdatedAtDesc(templateId)) : records.findByReporterIdOrderByUpdatedAtDesc(user.getId());
        return source.stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true) public List<ReportDtos.Summary> summaries() {
        User user = currentUsers.current(); boolean leadership = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        return leadership ? records.findSummaries() : records.findSummariesByReporterId(user.getId());
    }
    @Transactional(readOnly = true) public ReportDtos.PageResponse page(Long templateId, Long reporterId, int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 50");
        User user = currentUsers.current(); boolean leadership = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        PageRequest pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedAt"));
        Page<ReportRecord> source;
        if (leadership) {
            if (templateId != null && reporterId != null) source = records.findByTemplateIdAndReporterId(templateId, reporterId, pageable);
            else source = templateId == null ? records.findAll(pageable) : records.findByTemplateId(templateId, pageable);
        }
        else source = templateId == null ? records.findByReporterId(user.getId(), pageable) : records.findByReporterIdAndTemplateId(user.getId(), templateId, pageable);
        return new ReportDtos.PageResponse(source.getContent().stream().map(this::toResponse).toList(), source.getTotalElements(), source.getNumber(), source.getSize(), source.getTotalPages());
    }
    @Transactional(readOnly = true) public ReportDtos.Response get(Long id) { ReportRecord entity = find(id); assertCanRead(entity); return toResponse(entity); }
    public ReportRecord findForAudit(Long id) { ReportRecord entity = find(id); assertCanRead(entity); return entity; }
    @Transactional(readOnly = true) public List<ReportDtos.Response> query(Long templateId, String fieldKey, String value) {
        if (fieldKey == null || fieldKey.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "fieldKey is required");
        User user = currentUsers.current(); boolean leadership = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        return values.findByFieldKeyAndValueText(fieldKey, value == null ? "" : value.trim()).stream().map(com.sjtb.reporting.domain.ReportRecordValue::getRecord)
                .filter(record -> templateId == null || record.getTemplate().getId().equals(templateId))
                .filter(record -> leadership || record.getReporter().getId().equals(user.getId()))
                .map(this::toResponse).toList();
    }
    public ReportDtos.Response create(ReportDtos.Request request) {
        User reporter = currentUsers.current(); assertCanFill(reporter); ReportTemplate template = templates.find(request.templateId()); ReportTask task = request.taskId() == null ? null : tasks.find(request.taskId()); validateTask(task, template, reporter);
        ReportTemplateVersion version = resolveVersion(template, task, request.templateVersionId());
        Map<String, Object> data = applyDefaults(version, request.data());
        if (!template.isEnabled()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template is disabled"); validateData(version, data); validateUniqueValues(template, version, data, null);
        ReportStatus status = request.status() == null ? ReportStatus.DRAFT : request.status();
        validateReporterStatus(reporter, status);
        ReportRecord entity = new ReportRecord(); entity.setTemplate(template); entity.setTemplateVersion(version); entity.setTask(task); entity.setReporter(reporter); entity.setStatus(status); entity.setDataJson(writeData(data));
        ReportRecord saved = records.save(entity); syncValues(saved, version, data);
        if (task != null && (saved.getStatus() == ReportStatus.SUBMITTED || saved.getStatus() == ReportStatus.APPROVED)) tasks.markSubmitted(task, reporter);
        audit.record(saved, "CREATE", null, saved.getDataJson(), null);
        return toResponse(saved);
    }
    public List<ReportDtos.Response> createBatch(List<ReportDtos.Request> requests) {
        if (requests == null || requests.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "At least one report row is required");
        return requests.stream().map(this::create).toList();
    }
    public ReportDtos.Response update(Long id, ReportDtos.Request request) {
        ReportRecord entity = find(id); User user = currentUsers.current(); boolean leader = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        if (!leader && !entity.getReporter().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the reporter or leader may modify this record");
        if (!leader && entity.getStatus() != ReportStatus.DRAFT) throw new ApiException(HttpStatus.BAD_REQUEST, "Submitted reports must be changed through a change request");
        String before = entity.getDataJson(); ReportTemplate template = templates.find(request.templateId()); if (!template.isEnabled() && !leader) throw new ApiException(HttpStatus.BAD_REQUEST, "Template is disabled"); ReportTask task = request.taskId() == null ? entity.getTask() : tasks.find(request.taskId()); validateTask(task, template, user); ReportTemplateVersion version = resolveVersion(template, task, request.templateVersionId() == null && entity.getTemplateVersion() != null ? entity.getTemplateVersion().getId() : request.templateVersionId()); Map<String, Object> data = applyDefaults(version, request.data()); validateData(version, data); validateUniqueValues(template, version, data, entity.getId()); if (request.status() != null) validateReporterStatus(user, request.status()); entity.setTemplate(template); entity.setTemplateVersion(version); entity.setTask(task); entity.setDataJson(writeData(data)); if (request.status() != null) entity.setStatus(request.status()); ReportRecord saved = records.save(entity); syncValues(saved, version, data); audit.record(saved, "UPDATE", before, saved.getDataJson(), null); return toResponse(saved);
    }
    public ReportDtos.Response review(Long id, ReportDtos.ReviewRequest request) {
        ReportRecord entity = find(id); entity.setStatus(request.status()); entity.setReviewComment(request.reviewComment()); audit.record(entity, "REVIEW", entity.getDataJson(), entity.getDataJson(), request.reviewComment()); return toResponse(entity);
    }
    public void delete(Long id) { ReportRecord entity = find(id); User user = currentUsers.current(); boolean leader = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER); if (!leader && !entity.getReporter().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "Only the reporter or leader may delete this record"); if (!leader && entity.getStatus() != ReportStatus.DRAFT) throw new ApiException(HttpStatus.BAD_REQUEST, "Submitted reports must be changed through a change request"); audit.record(entity, "DELETE", entity.getDataJson(), null, null); if (changeRequests.existsByReportId(id)) changeRequests.deleteByReportId(id); records.delete(entity); }
    private ReportRecord find(Long id) { return records.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report record not found")); }
    private void assertCanRead(ReportRecord entity) { User user = currentUsers.current(); if (!user.getRoles().contains(Role.ADMIN) && !user.getRoles().contains(Role.LEADER) && !entity.getReporter().getId().equals(user.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this record"); }
    private void assertCanFill(User user) { if (!user.getRoles().contains(Role.ADMIN) && !user.getRoles().contains(Role.LEADER) && !user.getRoles().contains(Role.REPORTER) && !user.getRoles().contains(Role.EDITOR)) throw new ApiException(HttpStatus.FORBIDDEN, "Your role cannot submit report data"); }
    private void validateReporterStatus(User user, ReportStatus status) { if (!user.getRoles().contains(Role.ADMIN) && !user.getRoles().contains(Role.LEADER) && status != ReportStatus.DRAFT && status != ReportStatus.SUBMITTED) throw new ApiException(HttpStatus.FORBIDDEN, "Only a leader or administrator may set this report status"); }
    private void validateTask(ReportTask task, ReportTemplate template, User user) {
        if (task == null) return;
        if (!task.getTemplate().getId().equals(template.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Task and template do not match");
        boolean manager = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.LEADER);
        if (manager) return;
        if (!"PUBLISHED".equals(task.getStatus())) throw new ApiException(HttpStatus.BAD_REQUEST, "This task is not published");
        LocalDateTime now = LocalDateTime.now();
        if (task.getStartAt() != null && now.isBefore(task.getStartAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "This task has not started");
        if (task.getDeadline() != null && now.isAfter(task.getDeadline()) && !task.isAllowLate()) throw new ApiException(HttpStatus.BAD_REQUEST, "This task is past its deadline");
        if (!task.getAssignees().isEmpty() && task.getAssignees().stream().noneMatch(item -> item.getId().equals(user.getId()))) throw new ApiException(HttpStatus.FORBIDDEN, "You are not assigned to this task");
    }
    void validateData(ReportTemplateVersion version, Map<String, Object> data) {
        List<TemplateDtos.Column> columns = templates.columns(version); for (TemplateDtos.Column column : columns) {
            Object raw = data.get(column.key()); String value = raw == null ? "" : String.valueOf(raw).trim();
            if (column.required() && value.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Required field missing: " + column.label());
            if (!value.isBlank()) validateValue(column, value);
        }
        if (!columns.stream().map(TemplateDtos.Column::key).collect(java.util.stream.Collectors.toSet()).containsAll(data.keySet())) throw new ApiException(HttpStatus.BAD_REQUEST, "Data contains fields that are not defined by the template");
    }
    public void applyApprovedChange(ReportRecord record, Map<String, Object> data, String reason) {
        ReportTemplateVersion version = record.getTemplateVersion() == null ? templates.currentVersion(record.getTemplate()) : record.getTemplateVersion();
        Map<String, Object> resolved = applyDefaults(version, data);
        validateData(version, resolved); validateUniqueValues(record.getTemplate(), version, resolved, record.getId());
        String before = record.getDataJson(); record.setDataJson(writeData(resolved)); record.setStatus(ReportStatus.SUBMITTED);
        syncValues(record, version, resolved); audit.record(record, "APPROVE_CHANGE", before, record.getDataJson(), reason);
    }
    private void validateUniqueValues(ReportTemplate template, ReportTemplateVersion version, Map<String, Object> data, Long excludedRecordId) {
        for (TemplateDtos.Column column : templates.columns(version)) {
            if (!column.uniqueValue()) continue;
            Object raw = data.get(column.key()); String value = raw == null ? "" : String.valueOf(raw).trim();
            if (value.isBlank()) continue;
            boolean duplicate = values.findByFieldKeyAndValueTextAndRecord_Template_Id(column.key(), value, template.getId()).stream()
                    .anyMatch(item -> !item.getRecord().getId().equals(excludedRecordId));
            if (duplicate) throw new ApiException(HttpStatus.CONFLICT, "Field '" + column.label() + "' must be unique within this template");
        }
    }
    private void validateValue(TemplateDtos.Column column, String value) {
        if (column.maxLength() != null && value.length() > column.maxLength()) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' exceeds maximum length");
        if (column.pattern() != null && !column.pattern().isBlank()) try { if (!value.matches(column.pattern())) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' does not match required format"); } catch (java.util.regex.PatternSyntaxException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' has an invalid validation pattern"); }
        try {
            if ("number".equals(column.type()) || "money".equals(column.type())) {
                BigDecimal number = new BigDecimal(value.replace(",", ""));
                if (column.minValue() != null && number.compareTo(column.minValue()) < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' is below minimum value");
                if (column.maxValue() != null && number.compareTo(column.maxValue()) > 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' exceeds maximum value");
                if (column.scale() != null && number.stripTrailingZeros().scale() > column.scale()) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' exceeds decimal scale");
            }
            else if ("date".equals(column.type())) LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            else if ("month".equals(column.type())) YearMonth.parse(value, DateTimeFormatter.ofPattern("uuuu-MM"));
            else if ("year".equals(column.type()) && !value.matches("\\d{4}")) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' has invalid year format");
            else if ("select".equals(column.type()) && !column.options().contains(value)) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' contains an unsupported option");
            else if ("multiselect".equals(column.type()) && value.split(",").length > 0) for (String item : value.split(",")) if (!column.options().contains(item.trim())) throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' contains an unsupported option");
        } catch (NumberFormatException | DateTimeParseException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Field '" + column.label() + "' has invalid " + column.type() + " format"); }
    }
    private Map<String, Object> applyDefaults(ReportTemplateVersion version, Map<String, Object> source) {
        Map<String, Object> data = new java.util.LinkedHashMap<>(source == null ? Map.of() : source);
        for (TemplateDtos.Column column : templates.columns(version)) if ((data.get(column.key()) == null || String.valueOf(data.get(column.key())).isBlank()) && column.defaultValue() != null && !column.defaultValue().isBlank()) data.put(column.key(), column.defaultValue());
        return data;
    }
    private String writeData(Map<String, Object> data) { try { return mapper.writeValueAsString(data); } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Report data cannot be serialized"); } }
    private void syncValues(ReportRecord record, ReportTemplateVersion version, Map<String, Object> data) {
        values.deleteByRecordId(record.getId()); values.flush();
        List<ReportRecordValue> rows = new java.util.ArrayList<>();
        for (TemplateDtos.Column column : templates.columns(version)) {
            Object raw = data.get(column.key()); if (raw == null || String.valueOf(raw).isBlank()) continue;
            String text = String.valueOf(raw).trim(); ReportRecordValue row = new ReportRecordValue(); row.setRecord(record); row.setTemplateId(record.getTemplate().getId()); row.setFieldKey(column.key()); row.setValueText(text);
            if (column.uniqueValue()) { row.setUniqueMarker("U"); row.setValueHash(sha256(text)); }
            try { if ("number".equals(column.type()) || "money".equals(column.type())) row.setValueNumber(new BigDecimal(text.replace(",", ""))); else if ("date".equals(column.type())) row.setValueDate(LocalDate.parse(text)); }
            catch (RuntimeException e) { throw new IllegalStateException("Validated report value cannot be indexed for field " + column.key(), e); }
            rows.add(row);
        }
        try { values.saveAll(rows); values.flush(); }
        catch (DataIntegrityViolationException e) { throw new ApiException(HttpStatus.CONFLICT, "A unique template field value already exists"); }
    }
    private String sha256(String value) {
        try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(bytes); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
    }
    private ReportDtos.Response toResponse(ReportRecord entity) {
        try { ReportTemplateVersion version = entity.getTemplateVersion() == null ? templates.currentVersion(entity.getTemplate()) : entity.getTemplateVersion(); return new ReportDtos.Response(entity.getId(), entity.getTemplate().getId(), entity.getTemplate().getName(), version.getId(), version.getVersionNo(), entity.getTask() == null ? null : entity.getTask().getId(), entity.getTask() == null ? null : entity.getTask().getName(), entity.getReporter().getId(), entity.getReporter().getUsername(), mapper.readValue(entity.getDataJson(), new TypeReference<>() {}), entity.getStatus(), entity.getReviewComment(), entity.getCreatedAt(), entity.getUpdatedAt()); }
        catch (Exception e) { throw new IllegalStateException("Stored report data is corrupt", e); }
    }

    private ReportTemplateVersion resolveVersion(ReportTemplate template, ReportTask task, Long requestedVersionId) {
        ReportTemplateVersion version = requestedVersionId == null ? (task != null && task.getTemplateVersion() != null ? task.getTemplateVersion() : templates.currentVersion(template)) : templates.findVersion(requestedVersionId);
        if (!version.getTemplate().getId().equals(template.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Template version does not belong to template");
        if (task != null && task.getTemplateVersion() != null && !task.getTemplateVersion().getId().equals(version.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Report version must match task version");
        return version;
    }
}
