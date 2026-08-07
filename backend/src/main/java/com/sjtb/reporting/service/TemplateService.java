package com.sjtb.reporting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.ReportTask;
import com.sjtb.reporting.domain.ReportTemplate;
import com.sjtb.reporting.domain.ReportTemplateVersion;
import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportRecordRepository;
import com.sjtb.reporting.repository.ReportRecordValueRepository;
import com.sjtb.reporting.repository.ReportChangeRequestRepository;
import com.sjtb.reporting.repository.ReportTemplateRepository;
import com.sjtb.reporting.repository.ReportTemplateVersionRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.ReportTaskDetailRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class TemplateService {
    private final ReportTemplateRepository templates;
    private final ReportTemplateVersionRepository versions;
    private final ReportRecordRepository records;
    private final ReportRecordValueRepository recordValues;
    private final ReportChangeRequestRepository changeRequests;
    private final ReportTaskRepository tasks;
    private final ReportTaskDetailRepository taskDetails;
    private final ObjectMapper mapper;

    public TemplateService(ReportTemplateRepository templates, ReportTemplateVersionRepository versions,
                            ReportRecordRepository records, ReportRecordValueRepository recordValues, ReportChangeRequestRepository changeRequests, ReportTaskRepository tasks, ReportTaskDetailRepository taskDetails, ObjectMapper mapper) {
        this.templates = templates; this.versions = versions; this.records = records; this.recordValues = recordValues; this.changeRequests = changeRequests; this.tasks = tasks; this.taskDetails = taskDetails; this.mapper = mapper;
    }

    public List<TemplateDtos.Response> list() { return templates.findAll().stream().map(this::toResponse).toList(); }
    public TemplateDtos.Response get(Long id) { return toResponse(find(id)); }
    public List<TemplateDtos.VersionResponse> versions(Long templateId) {
        currentVersion(find(templateId));
        return versions.findByTemplateIdOrderByVersionNoDesc(templateId).stream()
                .map(version -> new TemplateDtos.VersionResponse(version.getId(), version.getVersionNo(), version.getStatus(), version.getCreatedAt(), columns(version)))
                .toList();
    }

    public TemplateDtos.Response create(TemplateDtos.Request request) {
        String code = normalizeCode(request.code(), request.name());
        if (templates.existsByCode(code)) throw new ApiException(HttpStatus.CONFLICT, "Template code already exists");
        validateColumns(request.columns());
        ReportTemplate entity = new ReportTemplate(); apply(entity, request, code);
        ReportTemplate saved = templates.save(entity);
        createVersion(saved, saved.getColumnsJson(), 1);
        return toResponse(saved);
    }

    /** Editing a template creates an immutable version; existing records retain their old schema. */
    public TemplateDtos.Response update(Long id, TemplateDtos.Request request) {
        ReportTemplate entity = find(id);
        String code = request.code() == null || request.code().isBlank() ? entity.getCode() : request.code();
        if (!entity.getCode().equals(code) && templates.existsByCode(code)) throw new ApiException(HttpStatus.CONFLICT, "Template code already exists");
        validateColumns(request.columns());
        String columnsJson = writeColumns(request.columns() == null ? List.of() : request.columns());
        apply(entity, request, code);
        int next = versions.findTopByTemplateIdOrderByVersionNoDesc(id).map(item -> item.getVersionNo() + 1).orElse(1);
        createVersion(entity, columnsJson, next);
        return toResponse(entity);
    }

    public void delete(Long id) {
        find(id);
        List<ReportTask> linkedTasks = tasks.findByTemplateId(id);
        // Records reference tasks; remove them first so the task foreign key cannot block template cleanup.
        records.findByTemplateId(id).forEach(record -> {
            recordValues.deleteByRecordId(record.getId());
            changeRequests.deleteByReportId(record.getId());
        });
        recordValues.flush(); changeRequests.flush();
        records.deleteAllByTemplateId(id);
        linkedTasks.forEach(task -> taskDetails.deleteByTaskId(task.getId()));
        if (!linkedTasks.isEmpty()) { tasks.deleteAll(linkedTasks); tasks.flush(); }
        versions.deleteAll(versions.findByTemplateIdOrderByVersionNoDesc(id)); versions.flush();
        templates.deleteById(id);
    }

    /** Returns the newest snapshot and lazily migrates legacy templates on first use. */
    public ReportTemplateVersion currentVersion(ReportTemplate template) {
        return versions.findTopByTemplateIdOrderByVersionNoDesc(template.getId()).orElseGet(() -> {
            ReportTemplateVersion version = createVersion(template, template.getColumnsJson(), 1);
            return version;
        });
    }

    public ReportTemplateVersion findVersion(Long id) {
        return versions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template version not found"));
    }

    public List<TemplateDtos.Column> columns(ReportTemplateVersion version) {
        try { return mapper.readValue(version.getColumnsJson(), new TypeReference<>() {}); }
        catch (Exception e) { throw new IllegalStateException("Stored template version JSON is corrupt", e); }
    }

    public ReportTemplate find(Long id) { return templates.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Template not found")); }

    private ReportTemplateVersion createVersion(ReportTemplate template, String columnsJson, int versionNo) {
        ReportTemplateVersion version = new ReportTemplateVersion(); version.setTemplate(template); version.setVersionNo(versionNo); version.setColumnsJson(columnsJson); version.setStatus("ACTIVE");
        return versions.save(version);
    }
    private void apply(ReportTemplate entity, TemplateDtos.Request request, String code) {
        String requestedStatus = request.status() == null || request.status().isBlank() ? entity.getStatus() : request.status();
        String status = requestedStatus == null || requestedStatus.isBlank() ? "PUBLISHED" : requestedStatus.trim().toUpperCase();
        if (!Set.of("DRAFT", "VALIDATING", "PUBLISHED", "IN_USE", "DISABLED").contains(status)) throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported template status");
        entity.setCode(code); entity.setName(request.name()); entity.setDescription(request.description()); entity.setStatus(status); entity.setEnabled((request.enabled() == null || request.enabled()) && !"DISABLED".equals(status));
        entity.setColumnsJson(writeColumns(request.columns() == null ? List.of() : request.columns()));
    }
    private String writeColumns(List<TemplateDtos.Column> columns) {
        try { return mapper.writeValueAsString(columns); } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid template columns"); }
    }
    private void validateColumns(List<TemplateDtos.Column> columns) {
        if (columns == null) return;
        HashSet<String> keys = new HashSet<>(); HashSet<String> labels = new HashSet<>(); Set<String> types = Set.of("text", "textarea", "number", "money", "date", "month", "year", "select", "multiselect", "attachment");
        for (TemplateDtos.Column column : columns) {
            if (column == null || column.key() == null || !column.key().matches("[A-Za-z][A-Za-z0-9_]{0,63}") || !keys.add(column.key())) throw new ApiException(HttpStatus.BAD_REQUEST, "Column keys must be unique letters, numbers, and underscores");
            if (column.label() == null || column.label().isBlank() || !labels.add(column.label().trim().toLowerCase(java.util.Locale.ROOT))) throw new ApiException(HttpStatus.BAD_REQUEST, "Column labels must be non-empty and unique");
            if (column.type() != null && !types.contains(column.type())) throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported column type: " + column.type());
            if (column.maxLength() != null && column.maxLength() < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "Column maxLength must be positive");
            if (column.scale() != null && (column.scale() < 0 || column.scale() > 8)) throw new ApiException(HttpStatus.BAD_REQUEST, "Column scale must be between 0 and 8");
            if (column.minValue() != null && column.maxValue() != null && column.minValue().compareTo(column.maxValue()) > 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Column minimum must not exceed maximum");
            if (("select".equals(column.type()) || "multiselect".equals(column.type())) && (column.options() == null || column.options().stream().map(String::trim).filter(value -> !value.isEmpty()).distinct().count() == 0)) throw new ApiException(HttpStatus.BAD_REQUEST, "Select columns require options");
            if (column.required() && Boolean.FALSE.equals(column.importable()) && (column.defaultValue() == null || column.defaultValue().isBlank())) throw new ApiException(HttpStatus.BAD_REQUEST, "Required non-importable column must define a default value: " + column.label());
        }
    }
    private String normalizeCode(String requested, String name) {
        if (requested != null && !requested.isBlank()) return requested;
        String base = name == null ? "template" : name.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("^-|-$", "").toLowerCase();
        if (base.isBlank()) base = "template";
        return base + "-" + System.currentTimeMillis();
    }
    private TemplateDtos.Response toResponse(ReportTemplate entity) {
        try {
            ReportTemplateVersion current = currentVersion(entity);
            return new TemplateDtos.Response(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(), mapper.readValue(current.getColumnsJson(), new TypeReference<>() {}), entity.isEnabled(), entity.getStatus(), current.getId(), current.getVersionNo());
        } catch (Exception e) { throw new IllegalStateException("Stored template JSON is corrupt", e); }
    }
}
