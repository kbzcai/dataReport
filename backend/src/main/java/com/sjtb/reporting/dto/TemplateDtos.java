package com.sjtb.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public final class TemplateDtos {
    private TemplateDtos() { }
    public record Column(@NotBlank String key, @NotBlank String label, String type, boolean required,
                         String defaultValue, Integer maxLength, BigDecimal minValue, BigDecimal maxValue, Integer scale,
                         String pattern, List<String> options, boolean uniqueValue, boolean searchable, boolean aggregatable,
                         boolean listVisible, boolean frozen, Boolean importable) {
        public Column(String key, String label, String type, boolean required) { this(key, label, type, required, null, null, null, null, null, null, List.of(), false, false, false, true, false, true); }
    }
    public record Request(String code, @NotBlank String name, String description, List<Column> columns, Boolean enabled, String status) {
        public Request(String code, String name, String description, List<Column> columns, Boolean enabled) { this(code, name, description, columns, enabled, null); }
    }
    public record Response(Long id, String code, String name, String description, List<Column> columns, boolean enabled, String status,
                           Long currentVersionId, int currentVersionNo) { }
    public record VersionResponse(Long id, int versionNo, String status, LocalDateTime createdAt, List<Column> columns) { }
}
