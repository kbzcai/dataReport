package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.ReportDtos;
import com.sjtb.reporting.dto.ChangeRequestDtos;
import com.sjtb.reporting.dto.ExcelDtos;
import com.sjtb.reporting.service.ChangeRequestService;
import com.sjtb.reporting.service.ExcelService;
import com.sjtb.reporting.service.ReportService;
import com.sjtb.reporting.service.ReportExportService;
import com.sjtb.reporting.service.ImportBatchService;
import com.sjtb.reporting.service.ReportAuditService;
import com.sjtb.reporting.domain.ReportChangeLog;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reports; private final ExcelService excel; private final ChangeRequestService changes; private final ReportExportService exports; private final ImportBatchService batches; private final ReportAuditService audit;
    public ReportController(ReportService reports, ExcelService excel, ChangeRequestService changes, ReportExportService exports, ImportBatchService batches, ReportAuditService audit) { this.reports = reports; this.excel = excel; this.changes = changes; this.exports = exports; this.batches = batches; this.audit = audit; }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ReportDtos.Response> list(@RequestParam(required = false) Long templateId) { return reports.list(templateId); }
    @GetMapping("/summary") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ReportDtos.Summary> summaries() { return reports.summaries(); }
    @GetMapping("/export") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public ResponseEntity<byte[]> export(@RequestParam Long templateId, @RequestParam(required = false) Long taskId) { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-export.xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(exports.export(templateId, taskId)); }
    @GetMapping("/page") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public ReportDtos.PageResponse page(@RequestParam(required = false) Long templateId, @RequestParam(required = false) Long reporterId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return reports.page(templateId, reporterId, page, size); }
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public ReportDtos.Response get(@PathVariable Long id) { return reports.get(id); }
    @GetMapping("/{id}/change-logs") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ReportChangeLog> changeLogs(@PathVariable Long id) { com.sjtb.reporting.domain.ReportRecord record = reports.findForAudit(id); return audit.list(record); }
    @GetMapping("/query") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ReportDtos.Response> query(@RequestParam(required = false) Long templateId, @RequestParam String fieldKey, @RequestParam(required = false) String value) { return reports.query(templateId, fieldKey, value); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public ReportDtos.Response create(@Valid @RequestBody ReportDtos.Request request) { return reports.create(request); }
    @PostMapping("/{id}/change-requests") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('REPORTER')") public ChangeRequestDtos.Response createChangeRequest(@PathVariable Long id, @Valid @RequestBody ChangeRequestDtos.CreateRequest request) { return changes.create(id, request); }
    @PostMapping("/batch") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public List<ReportDtos.Response> createBatch(@Valid @RequestBody List<ReportDtos.Request> requests) { return reports.createBatch(requests); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public ReportDtos.Response update(@PathVariable Long id, @Valid @RequestBody ReportDtos.Request request) { return reports.update(id, request); }
    @PatchMapping("/{id}/review") @PreAuthorize("hasRole('ADMIN')") public ReportDtos.Response review(@PathVariable Long id, @Valid @RequestBody ReportDtos.ReviewRequest request) { return reports.review(id, request); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public void delete(@PathVariable Long id) { reports.delete(id); }
    @PostMapping("/import-preview") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public ExcelDtos.PreviewResponse importPreview(@RequestParam("file") MultipartFile file) { return excel.previewImport(file); }
    @PostMapping("/import-confirm") @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public ExcelDtos.ImportResult importConfirm(@RequestParam("file") MultipartFile file, @RequestParam String mapping) { return excel.confirmImport(file, mapping); }
    @PostMapping("/import") @PreAuthorize("hasAnyRole('ADMIN','REPORTER')") public ExcelDtos.ImportResult importExcel(@RequestParam(required = false) Long templateId, @RequestParam(required = false) Long taskId, @RequestParam("file") MultipartFile file) { return templateId == null ? excel.importWorkbook(file) : excel.importRecords(templateId, taskId, file); }
    @GetMapping("/import-batches") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ExcelDtos.BatchResponse> importBatches() { return batches.list(); }
    @GetMapping("/import-batches/{id}/errors/download") @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public ResponseEntity<byte[]> downloadImportErrors(@PathVariable Long id) { return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-errors-" + id + ".csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(batches.errorCsv(id)); }
}
