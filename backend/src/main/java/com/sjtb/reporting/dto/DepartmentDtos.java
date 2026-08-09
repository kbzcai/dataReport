package com.sjtb.reporting.dto;

import jakarta.validation.constraints.NotBlank;

public final class DepartmentDtos {
    private DepartmentDtos() { }
    public record Request(@NotBlank String name, Long parentId) { }
    public record Response(Long id, String name, Long parentId) { }
}
