package com.sjtb.reporting.dto;

import java.util.List;
import java.time.LocalDateTime;

public final class ExcelDtos {
    private ExcelDtos() { }

    public record PreviewResponse(List<SheetPreview> sheets) { }

    public record SheetPreview(int sheetOrder, int sheetIndex, String sheetName,
                               Long suggestedTemplateId, String suggestedTemplateName,
                               String matchStatus) { }
    public record ImportResult(Long batchId, int importedRows, int failedRows, String status) { }
    public record BatchResponse(Long id, String originalFileName, String creatorName, String status,
                                int importedRows, int failedRows, String summary, LocalDateTime createdAt, LocalDateTime completedAt) { }
    public record ErrorResponse(String sheetName, Integer rowNumber, String message) { }
}
