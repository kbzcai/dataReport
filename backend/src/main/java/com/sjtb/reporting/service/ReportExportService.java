package com.sjtb.reporting.service;

import com.sjtb.reporting.dto.ReportDtos;
import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.exception.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReportExportService {
    private final ReportService reports; private final TemplateService templates;
    public ReportExportService(ReportService reports, TemplateService templates) { this.reports = reports; this.templates = templates; }
    public byte[] export(Long templateId, Long taskId) {
        if (templateId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "A template is required for Excel export");
        List<ReportDtos.Response> data = reports.list(templateId).stream().filter(record -> taskId == null || taskId.equals(record.taskId())).toList();
        TemplateDtos.Response template = templates.get(templateId);
        List<TemplateDtos.Column> columns = taskId != null && !data.isEmpty()
                ? templates.columns(templates.findVersion(data.get(0).templateVersionId())) : template.columns();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(template.name()); Row header = sheet.createRow(0); int column = 0;
            for (String title : List.of("任务", "填报人", "状态", "提交时间")) header.createCell(column++).setCellValue(title);
            for (TemplateDtos.Column item : columns) header.createCell(column++).setCellValue(item.label());
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                ReportDtos.Response record = data.get(rowIndex); Row row = sheet.createRow(rowIndex + 1); int cell = 0;
                row.createCell(cell++).setCellValue(record.taskName() == null ? "" : record.taskName()); row.createCell(cell++).setCellValue(record.reporterName()); row.createCell(cell++).setCellValue(record.status().name()); row.createCell(cell++).setCellValue(record.updatedAt().toString());
                Map<String, Object> values = record.data(); for (TemplateDtos.Column item : columns) row.createCell(cell++).setCellValue(String.valueOf(values.getOrDefault(item.key(), "")));
            }
            for (int i = 0; i < column; i++) sheet.setColumnWidth(i, 20 * 256);
            workbook.write(out); return out.toByteArray();
        } catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot generate report export"); }
    }
}
