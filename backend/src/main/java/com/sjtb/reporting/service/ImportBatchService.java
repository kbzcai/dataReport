package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.*;
import com.sjtb.reporting.dto.ExcelDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.ReportImportBatchRepository;
import com.sjtb.reporting.repository.ReportImportErrorRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportBatchService {
    private static final Pattern ROW_ERROR = Pattern.compile("Sheet '([^']+)', row (\\d+): (.*)");
    private final ReportImportBatchRepository batches;
    private final ReportImportErrorRepository errors;
    private final CurrentUserService current; private final AccessControlService access;
    public ImportBatchService(ReportImportBatchRepository batches, ReportImportErrorRepository errors, CurrentUserService current, AccessControlService access) { this.batches = batches; this.errors = errors; this.current = current; this.access = access; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long start(String fileName) {
        User creator = current.current(); access.requireEdit(creator);
        ReportImportBatch batch = new ReportImportBatch(); batch.setOriginalFileName(fileName == null || fileName.isBlank() ? "import.xlsx" : fileName); batch.setCreator(creator);
        return batches.save(batch).getId();
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExcelDtos.ImportResult complete(Long id, int importedRows) {
        ReportImportBatch batch = find(id); batch.setStatus("SUCCESS"); batch.setImportedRows(importedRows); batch.setFailedRows(0); batch.setSummary("导入成功 " + importedRows + " 行"); batch.setCompletedAt(LocalDateTime.now());
        return new ExcelDtos.ImportResult(batch.getId(), importedRows, 0, batch.getStatus());
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long id, String detail) {
        ReportImportBatch batch = find(id); batch.setStatus("FAILED"); batch.setFailedRows(1); batch.setSummary(trim(detail)); batch.setCompletedAt(LocalDateTime.now());
        ReportImportError error = new ReportImportError(); error.setBatch(batch); error.setMessage(trim(detail));
        Matcher matcher = ROW_ERROR.matcher(detail == null ? "" : detail);
        if (matcher.matches()) { error.setSheetName(matcher.group(1)); error.setRowNumber(Integer.valueOf(matcher.group(2))); }
        errors.save(error);
    }
    @Transactional(readOnly = true)
    public List<ExcelDtos.BatchResponse> list() {
        User user = current.current(); access.requireView(user); java.util.Set<Long> scope = access.scopeDepartmentIds(user);
        List<ReportImportBatch> source = batches.findAllByOrderByCreatedAtDesc().stream()
                .filter(batch -> batch.getCreator().getId().equals(user.getId()) || access.canManageDepartment(user, batch.getCreator().getDepartment(), scope)).toList();
        return source.stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public byte[] errorCsv(Long id) {
        ReportImportBatch batch = find(id); assertVisible(batch);
        StringBuilder text = new StringBuilder("工作表,行号,错误信息\r\n");
        for (ReportImportError error : errors.findByBatchIdOrderByIdAsc(id)) text.append(csv(error.getSheetName())).append(',').append(error.getRowNumber() == null ? "" : error.getRowNumber()).append(',').append(csv(error.getMessage())).append("\r\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream(); output.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); output.writeBytes(text.toString().getBytes(StandardCharsets.UTF_8)); return output.toByteArray();
    }
    private ReportImportBatch find(Long id) { return batches.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Import batch not found")); }
    private void assertVisible(ReportImportBatch batch) { User user = current.current(); if (!access.hasView(user) || (!batch.getCreator().getId().equals(user.getId()) && !access.canManageDepartment(user, batch.getCreator().getDepartment()))) throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this import batch"); }
    private ExcelDtos.BatchResponse response(ReportImportBatch batch) { return new ExcelDtos.BatchResponse(batch.getId(), batch.getOriginalFileName(), batch.getCreator().getUsername(), batch.getStatus(), batch.getImportedRows(), batch.getFailedRows(), batch.getSummary(), batch.getCreatedAt(), batch.getCompletedAt()); }
    private static String trim(String value) { if (value == null || value.isBlank()) return "导入失败"; return value.length() > 1000 ? value.substring(0, 1000) : value; }
    private static String csv(String value) { return '"' + (value == null ? "" : value.replace("\"", "\"\"")) + '"'; }
}
