package com.sjtb.reporting.controller;

import com.sjtb.reporting.domain.ChangeRequestStatus;
import com.sjtb.reporting.dto.ChangeRequestDtos;
import com.sjtb.reporting.service.ChangeRequestService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/change-requests")
public class ChangeRequestController {
    private final ChangeRequestService service;
    public ChangeRequestController(ChangeRequestService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<ChangeRequestDtos.Response> list(@RequestParam(required = false) ChangeRequestStatus status) { return service.list(status); }
    @PatchMapping("/{id}/approve") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public ChangeRequestDtos.Response approve(@PathVariable Long id, @RequestBody(required = false) ChangeRequestDtos.ReviewRequest input) { return service.approve(id, input); }
    @PatchMapping("/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public ChangeRequestDtos.Response reject(@PathVariable Long id, @RequestBody(required = false) ChangeRequestDtos.ReviewRequest input) { return service.reject(id, input); }
    @PatchMapping("/{id}/cancel") @PreAuthorize("hasRole('REPORTER')") public ChangeRequestDtos.Response cancel(@PathVariable Long id) { return service.cancel(id); }
}
