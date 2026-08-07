package com.sjtb.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sjtb.reporting.dto.ExcelDtos;
import com.sjtb.reporting.dto.ReportDtos;
import com.sjtb.reporting.dto.TemplateDtos;
import com.sjtb.reporting.dto.TemplateImportDtos;
import com.sjtb.reporting.exception.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelService {
    private final TemplateService templates; private final ReportService reports; private final ImportBatchService batches; private final ObjectMapper mapper;
    public ExcelService(TemplateService templates, ReportService reports, ImportBatchService batches, ObjectMapper mapper) { this.templates = templates; this.reports = reports; this.batches = batches; this.mapper = mapper; }
    public byte[] blankTemplate(Long templateId) {
        TemplateDtos.Response template = templates.get(templateId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(template.name()); Row header = sheet.createRow(0);
            List<TemplateDtos.Column> columns = template.columns().stream().filter(column -> !Boolean.FALSE.equals(column.importable())).toList();
            if (columns.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template has no importable fields");
            for (int i = 0; i < columns.size(); i++) { header.createCell(i).setCellValue(columns.get(i).label()); sheet.setColumnWidth(i, 20 * 256); }
            workbook.write(out); return out.toByteArray();
        } catch (IOException e) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create Excel template"); }
    }
    public TemplateDtos.Response importTemplate(String code, String name, String description, MultipartFile file) {
        if (code == null || !code.matches("[A-Za-z][A-Za-z0-9_-]{1,63}") || name == null || name.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template code and name are required");
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel file is required");
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<TemplateDtos.Column> columns = ExcelHeaderParser.inferWorkbookColumns(workbook);
            return templates.create(new TemplateDtos.Request(code, name, description, columns, true));
        } catch (ApiException e) { throw e; } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Excel template import failed: use a valid .xlsx file"); }
    }

    @Transactional(readOnly = true)
    public TemplateImportDtos.PreviewResponse previewTemplateImport(MultipartFile file) {
        validateImportFile(file);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<TemplateImportDtos.SheetPreview> sheets = new ArrayList<>();
            int order = 0;
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                Sheet sheet = workbook.getSheetAt(index);
                boolean valid = true;
                String message = "表头校验通过，无数据内容";
                try {
                    ExcelHeaderParser.inferTemplateColumns(sheet);
                } catch (ApiException e) {
                    valid = false;
                    message = e.getMessage();
                }
                sheets.add(new TemplateImportDtos.SheetPreview(order++, index, sheet.getSheetName(), valid, message));
            }
            if (sheets.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no sheets");
            return new TemplateImportDtos.PreviewResponse(sheets);
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Excel template preview failed: use a valid .xlsx file"); }
    }

    @Transactional
    public List<TemplateDtos.Response> importTemplates(String namesJson, MultipartFile file) {
        validateImportFile(file);
        List<String> names = parseTemplateNames(namesJson);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (names.size() != workbook.getNumberOfSheets()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template names must match all Sheet count and order");
            List<TemplateDtos.Response> created = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                Sheet sheet = workbook.getSheetAt(index);
                List<TemplateDtos.Column> columns;
                try { columns = ExcelHeaderParser.inferTemplateColumns(sheet); }
                catch (ApiException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "': " + e.getMessage()); }
                String code = "excel_import_" + System.currentTimeMillis() + "_" + index;
                created.add(templates.create(new TemplateDtos.Request(code, names.get(index), "从 Excel 工作表导入", columns, true)));
            }
            return created;
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Excel template import failed: use a valid .xlsx file"); }
    }

    private List<String> parseTemplateNames(String namesJson) {
        if (namesJson == null || namesJson.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template names are required");
        try {
            JsonNode root = mapper.readTree(namesJson);
            if (!root.isArray() || root.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template names must be a non-empty JSON array");
            List<String> names = new ArrayList<>();
            for (JsonNode node : root) {
                String name = node.isTextual() ? node.asText().trim() : "";
                if (name.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Every Sheet must have a template name");
                names.add(name);
            }
            return names;
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Template names JSON is invalid"); }
    }

    public TemplateDtos.Response replaceTemplateColumns(Long templateId, MultipartFile file) {
        ReportTemplateEntityAdapter existing = new ReportTemplateEntityAdapter(templates.find(templateId));
        List<TemplateDtos.Column> columns = readHeaderOnly(file);
        return templates.update(templateId, new TemplateDtos.Request(existing.code(), existing.name(), existing.description(), columns, existing.enabled()));
    }

    private List<TemplateDtos.Column> readHeaderOnly(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel file is required");
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel sheet is missing");
            return ExcelHeaderParser.inferWorkbookColumns(workbook);
        } catch (ApiException e) { throw e; } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Excel template import failed: use a valid .xlsx file"); }
    }

    private record ReportTemplateEntityAdapter(String code, String name, String description, boolean enabled) {
        ReportTemplateEntityAdapter(com.sjtb.reporting.domain.ReportTemplate template) { this(template.getCode(), template.getName(), template.getDescription(), template.isEnabled()); }
    }

    /**
     * Imports a workbook whose non-empty sheets may belong to different templates.
     * Sheet name/code matching is preferred; headers are used as a unique fallback.
     */
    @Transactional
    public ExcelDtos.ImportResult importWorkbook(MultipartFile file) {
        validateImportFile(file);
        Long batchId = batches.start(file.getOriginalFilename());
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<TemplateDtos.Response> available = templates.list().stream().filter(TemplateDtos.Response::enabled).toList();
            int count = 0;
            boolean hasDataSheet = false;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (ExcelHeaderParser.isBlankSheet(sheet)) continue;
                hasDataSheet = true;
                TemplateDtos.Response template = resolveTemplate(sheet, available);
                count += importSheet(template, sheet);
            }
            if (!hasDataSheet || count == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no data rows across its sheets");
            return batches.complete(batchId, count);
        } catch (ApiException e) { batches.fail(batchId, e.getMessage()); throw e; }
        catch (Exception e) { String message = "Excel import failed: use a valid .xlsx file"; batches.fail(batchId, message); throw new ApiException(HttpStatus.BAD_REQUEST, message); }
    }

    @Transactional
    public ExcelDtos.ImportResult importRecords(Long templateId, Long taskId, MultipartFile file) {
        validateImportFile(file);
        Long batchId = batches.start(file.getOriginalFilename());
        int count = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            TemplateDtos.Response template = templates.get(templateId);
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (ExcelHeaderParser.isBlankSheet(sheet)) continue;
                count += importSheet(template, taskId, sheet);
            }
            if (count == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no data rows across its sheets");
            return batches.complete(batchId, count);
        } catch (ApiException e) { batches.fail(batchId, e.getMessage()); throw e; } catch (Exception e) { String message = "Excel import failed: use a valid .xlsx file"; batches.fail(batchId, message); throw new ApiException(HttpStatus.BAD_REQUEST, message); }
    }

    @Transactional(readOnly = true)
    public ExcelDtos.PreviewResponse previewImport(MultipartFile file) {
        validateImportFile(file);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<TemplateDtos.Response> available = templates.list().stream().filter(TemplateDtos.Response::enabled).toList();
            List<ExcelDtos.SheetPreview> sheets = new ArrayList<>();
            int order = 0;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (ExcelHeaderParser.isBlankSheet(sheet)) continue;
                MatchSuggestion suggestion = suggestTemplate(sheet, available);
                TemplateDtos.Response template = suggestion.template();
                sheets.add(new ExcelDtos.SheetPreview(order++, sheetIndex, sheet.getSheetName(),
                        template == null ? null : template.id(), template == null ? null : template.name(), suggestion.status()));
            }
            if (sheets.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no non-empty sheets");
            return new ExcelDtos.PreviewResponse(sheets);
        } catch (ApiException e) { throw e; }
        catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Excel preview failed: use a valid .xlsx file"); }
    }

    @Transactional
    public ExcelDtos.ImportResult confirmImport(MultipartFile file, String mappingJson) {
        validateImportFile(file);
        Long batchId = batches.start(file.getOriginalFilename());
        ImportMapping mapping = parseImportMapping(mappingJson);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            List<TemplateDtos.Response> available = templates.list().stream().filter(TemplateDtos.Response::enabled).toList();
            List<ConfirmedSheet> confirmed = new ArrayList<>();
            int order = 0;
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (ExcelHeaderParser.isBlankSheet(sheet)) continue;
                Long templateId = mapping.orderMapping().get(order);
                if (templateId == null) templateId = mapping.nameMapping().get(sheet.getSheetName());
                if (templateId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "' is missing a template mapping");
                Long resolvedTemplateId = templateId;
                TemplateDtos.Response template = available.stream().filter(item -> item.id().equals(resolvedTemplateId)).findFirst().orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "' maps to an unavailable or disabled template: " + resolvedTemplateId));
                try { ExcelHeaderParser.match(sheet, template.columns()); }
                catch (ApiException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "': " + e.getMessage()); }
                confirmed.add(new ConfirmedSheet(template, sheet));
                order++;
            }
            if (confirmed.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no non-empty sheets");
            int count = 0;
            for (ConfirmedSheet item : confirmed) count += importSheet(item.template(), item.sheet());
            if (count == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no data rows across its sheets");
            return batches.complete(batchId, count);
        } catch (ApiException e) { batches.fail(batchId, e.getMessage()); throw e; }
        catch (Exception e) { String message = "Excel confirmation import failed: use a valid .xlsx file"; batches.fail(batchId, message); throw new ApiException(HttpStatus.BAD_REQUEST, message); }
    }

    private int importSheet(TemplateDtos.Response template, Sheet sheet) {
        return importSheet(template, null, sheet);
    }

    private int importSheet(TemplateDtos.Response template, Long taskId, Sheet sheet) {
        ExcelHeaderParser.Match match;
        try { match = ExcelHeaderParser.match(sheet, template.columns()); }
        catch (ApiException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "': " + e.getMessage()); }
        DataFormatter formatter = new DataFormatter();
        int count = 0;
        for (int rowNum = match.headerEndRow() + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null || ExcelHeaderParser.isBlank(row, formatter)) continue;
            Map<String, Object> data = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> item : match.indexToKey().entrySet()) {
                Cell cell = row.getCell(item.getKey(), Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                data.put(item.getValue(), formatter.formatCellValue(cell).trim());
            }
            try { reports.create(new ReportDtos.Request(template.id(), taskId, data, com.sjtb.reporting.domain.ReportStatus.SUBMITTED)); }
            catch (ApiException e) { throw new ApiException(e.getStatus(), "Sheet '" + sheet.getSheetName() + "', row " + (rowNum + 1) + ": " + e.getMessage()); }
            count++;
        }
        return count;
    }

    private TemplateDtos.Response resolveTemplate(Sheet sheet, List<TemplateDtos.Response> available) {
        MatchSuggestion suggestion = suggestTemplate(sheet, available);
        if (suggestion.template() != null) return suggestion.template();
        String reason = "AMBIGUOUS".equals(suggestion.status()) ? "matches multiple templates" : "cannot be matched to an enabled template by name, code, or headers";
        throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "' " + reason);
    }

    private MatchSuggestion suggestTemplate(Sheet sheet, List<TemplateDtos.Response> available) {
        String sheetName = sheet.getSheetName().trim();
        List<TemplateDtos.Response> nameMatches = available.stream().filter(template -> equalsIgnoreCase(sheetName, template.name()) || equalsIgnoreCase(sheetName, template.code())).toList();
        if (nameMatches.size() == 1) return new MatchSuggestion(nameMatches.get(0), "NAME");
        if (nameMatches.size() > 1) return new MatchSuggestion(null, "AMBIGUOUS");
        List<TemplateDtos.Response> headerMatches = new ArrayList<>();
        for (TemplateDtos.Response template : available) {
            try { ExcelHeaderParser.match(sheet, template.columns()); headerMatches.add(template); }
            catch (ApiException e) { /* Candidate probe: this template does not match the sheet headers. */ }
        }
        if (headerMatches.size() == 1) return new MatchSuggestion(headerMatches.get(0), "HEADER");
        return new MatchSuggestion(null, headerMatches.isEmpty() ? "UNMATCHED" : "AMBIGUOUS");
    }

    private ImportMapping parseImportMapping(String json) {
        if (json == null || json.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "Import mapping JSON is required");
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || (!root.isArray() && !root.isObject())) throw new ApiException(HttpStatus.BAD_REQUEST, "Import mapping must be a JSON array or object");
            Map<Integer, Long> orderMapping = new HashMap<>();
            Map<String, Long> nameMapping = new HashMap<>();
            if (root.isArray()) {
                for (int i = 0; i < root.size(); i++) orderMapping.put(i, readTemplateId(root.get(i), "mapping[" + i + "]"));
            } else {
                root.fields().forEachRemaining(entry -> {
                    String key = entry.getKey();
                    long templateId = readTemplateId(entry.getValue(), "mapping['" + key + "']");
                    if (key.matches("\\d+")) orderMapping.put(Integer.parseInt(key), templateId);
                    else nameMapping.put(key, templateId);
                });
            }
            return new ImportMapping(orderMapping, nameMapping);
        }
        catch (ApiException e) { throw e; } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Import mapping JSON is invalid"); }
    }

    private long readTemplateId(JsonNode node, String path) {
        if (node == null || !node.isIntegralNumber() || node.longValue() <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, path + " must be a positive template id");
        return node.longValue();
    }

    private record MatchSuggestion(TemplateDtos.Response template, String status) { }
    private record ConfirmedSheet(TemplateDtos.Response template, Sheet sheet) { }
    private record ImportMapping(Map<Integer, Long> orderMapping, Map<String, Long> nameMapping) { }

    private static boolean equalsIgnoreCase(String left, String right) { return right != null && left.equalsIgnoreCase(right.trim()); }

    private static void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel file is required");
    }
}
