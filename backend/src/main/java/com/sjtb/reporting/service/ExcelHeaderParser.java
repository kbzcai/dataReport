package com.sjtb.reporting.service;

import com.sjtb.reporting.dto.TemplateDtos;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.http.HttpStatus;
import com.sjtb.reporting.exception.ApiException;

/** Resolves simple, multi-row, and merged Excel headers into template columns. */
final class ExcelHeaderParser {
    private ExcelHeaderParser() { }

    record Match(Map<Integer, String> indexToKey, int headerEndRow) { }

    static Match match(Sheet sheet, List<TemplateDtos.Column> columns) {
        if (columns == null || columns.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template has no fields");
        List<TemplateDtos.Column> importableColumns = columns.stream().filter(column -> !Boolean.FALSE.equals(column.importable())).toList();
        if (importableColumns.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Template has no importable fields");
        DataFormatter formatter = new DataFormatter();
        int maxRow = Math.max(0, sheet.getLastRowNum());
        int maxColumn = maxColumn(sheet);
        for (int endRow = 0; endRow <= Math.min(9, maxRow); endRow++) {
            Map<Integer, String> mapping = new LinkedHashMap<>();
            Set<String> usedKeys = new HashSet<>();
            for (int columnIndex = 0; columnIndex <= maxColumn; columnIndex++) {
                String path = headerPath(sheet, columnIndex, endRow, formatter);
                if (path.isBlank()) continue;
                TemplateDtos.Column best = null;
                int bestScore = 0;
                for (TemplateDtos.Column column : importableColumns) {
                    if (usedKeys.contains(column.key())) continue;
                    int score = matchScore(path, column.label());
                    if (score > bestScore) { best = column; bestScore = score; }
                }
                if (best != null) { mapping.put(columnIndex, best.key()); usedKeys.add(best.key()); }
            }
            if (mapping.size() == importableColumns.size()) {
                for (TemplateDtos.Column column : columns) if (Boolean.FALSE.equals(column.importable())) {
                    for (int columnIndex = 0; columnIndex <= maxColumn; columnIndex++) if (matchScore(headerPath(sheet, columnIndex, endRow, formatter), column.label()) > 0) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains a non-importable field: " + column.label());
                    }
                }
                return new Match(mapping, endRow);
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Excel headers do not match the template; supported headers may span merged or multiple rows");
    }

    static String normalizeHeader(String value) { return normalize(value); }

    static List<TemplateDtos.Column> inferTemplateColumns(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        int headerEnd = inferredHeaderEnd(sheet);
        int maxColumn = maxColumn(sheet);
        List<TemplateDtos.Column> columns = new ArrayList<>();
        Set<String> labels = new HashSet<>();
        for (int columnIndex = 0; columnIndex <= maxColumn; columnIndex++) {
            String label = headerPath(sheet, columnIndex, headerEnd, formatter);
            if (label.isBlank()) continue;
            if (!labels.add(normalize(label))) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel headers must be non-empty and unique");
            String leaf = label.substring(label.lastIndexOf(" / ") + 3).trim();
            String key = leaf.matches("[A-Za-z][A-Za-z0-9_]{0,63}") ? leaf : "column_" + (columnIndex + 1);
            columns.add(new TemplateDtos.Column(key, label, "text", false));
        }
        if (columns.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel must contain at least one header");
        for (int rowNum = headerEnd + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null && !isBlank(row, formatter)) throw new ApiException(HttpStatus.BAD_REQUEST, "Template Excel must contain headers only and no data rows");
        }
        return columns;
    }

    static List<TemplateDtos.Column> inferWorkbookColumns(Workbook workbook) {
        Sheet first = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) if (!isBlankSheet(workbook.getSheetAt(i))) { first = workbook.getSheetAt(i); break; }
        if (first == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Excel contains no non-empty sheets");
        List<TemplateDtos.Column> columns = inferTemplateColumns(first);
        Set<String> expected = new LinkedHashSet<>(); columns.forEach(column -> expected.add(normalize(column.label())));
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet == first || isBlankSheet(sheet)) continue;
            List<TemplateDtos.Column> actual = inferTemplateColumns(sheet); Set<String> labels = new LinkedHashSet<>(); actual.forEach(column -> labels.add(normalize(column.label())));
            if (!expected.equals(labels)) throw new ApiException(HttpStatus.BAD_REQUEST, "Sheet '" + sheet.getSheetName() + "' has a different template header");
        }
        return columns;
    }

    static boolean isBlank(Row row, DataFormatter formatter) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).trim().isEmpty()) return false;
        return true;
    }

    static boolean isBlankSheet(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        for (Row row : sheet) if (!isBlank(row, formatter)) return false;
        return true;
    }

    private static int inferredHeaderEnd(Sheet sheet) {
        int lastMergedRow = -1;
        for (CellRangeAddress range : sheet.getMergedRegions()) lastMergedRow = Math.max(lastMergedRow, range.getLastRow());
        if (lastMergedRow >= 0) return lastMergedRow;
        Row first = sheet.getRow(0); Row second = sheet.getRow(1);
        if (second != null && nonBlankCount(first) <= 1 && nonBlankCount(second) >= 2 && allText(second)) return 1;
        return 0;
    }

    private static String headerPath(Sheet sheet, int columnIndex, int endRow, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        for (int rowIndex = 0; rowIndex <= endRow; rowIndex++) {
            String value = cellText(sheet, rowIndex, columnIndex, formatter);
            if (!value.isBlank() && (values.isEmpty() || !normalize(values.get(values.size() - 1)).equals(normalize(value)))) values.add(value);
        }
        return String.join(" / ", values);
    }

    private static String cellText(Sheet sheet, int rowIndex, int columnIndex, DataFormatter formatter) {
        Cell direct = Optional.ofNullable(sheet.getRow(rowIndex)).map(row -> row.getCell(columnIndex)).orElse(null);
        if (direct != null) {
            String value = formatter.formatCellValue(direct).trim();
            if (!value.isBlank()) return value;
        }
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (range.isInRange(rowIndex, columnIndex)) {
                Cell topLeft = sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn());
                return formatter.formatCellValue(topLeft).trim();
            }
        }
        return "";
    }

    private static int maxColumn(Sheet sheet) {
        int max = 0;
        for (Row row : sheet) max = Math.max(max, row.getLastCellNum() - 1);
        for (CellRangeAddress range : sheet.getMergedRegions()) max = Math.max(max, range.getLastColumn());
        return max;
    }

    private static int matchScore(String path, String label) {
        String pathNorm = normalize(path); String labelNorm = normalize(label);
        if (pathNorm.equals(labelNorm)) return 3;
        for (String segment : path.split(" / ")) if (normalize(segment).equals(labelNorm)) return 2;
        if (pathNorm.endsWith(labelNorm) || labelNorm.endsWith(pathNorm)) return 1;
        return 0;
    }

    private static String normalize(String value) { return value == null ? "" : value.replaceAll("[\\s\\p{Punct}、，：:；;（）()【】\\[\\]/\\\\_-]+", "").toLowerCase(Locale.ROOT); }
    private static int nonBlankCount(Row row) { if (row == null) return 0; int count = 0; for (Cell cell : row) if (!cell.toString().isBlank()) count++; return count; }
    private static boolean allText(Row row) { for (Cell cell : row) if (cell.getCellType() == CellType.NUMERIC) return false; return true; }
}
