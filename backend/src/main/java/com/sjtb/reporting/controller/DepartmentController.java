package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.DepartmentDtos;
import com.sjtb.reporting.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/departments") @PreAuthorize("hasAnyRole('ADMIN','LEADER')")
public class DepartmentController {
    private final DepartmentService departments;
    public DepartmentController(DepartmentService departments) { this.departments = departments; }
    @GetMapping public List<DepartmentDtos.Response> list() { return departments.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')") public DepartmentDtos.Response create(@Valid @RequestBody DepartmentDtos.Request request) { return departments.create(request); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public DepartmentDtos.Response update(@PathVariable Long id, @Valid @RequestBody DepartmentDtos.Request request) { return departments.update(id, request); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void delete(@PathVariable Long id) { departments.delete(id); }
}
