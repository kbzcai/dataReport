package com.sjtb.reporting.dto;

import java.util.List;

public final class TemplateImportDtos {
    private TemplateImportDtos() { }

    public record SheetPreview(int sheetOrder, int sheetIndex, String sheetName, boolean valid, String message) { }
    public record PreviewResponse(List<SheetPreview> sheets) { }
}
