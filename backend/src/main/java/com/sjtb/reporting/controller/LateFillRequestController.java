package com.sjtb.reporting.controller;
import com.sjtb.reporting.dto.LateFillDtos;
import com.sjtb.reporting.service.LateFillRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/late-fill-requests") public class LateFillRequestController { private final LateFillRequestService service; public LateFillRequestController(LateFillRequestService s){service=s;} @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<LateFillDtos.Response> list(){return service.list();} @GetMapping("/leaders") @PreAuthorize("hasRole('REPORTER')") public List<LateFillDtos.Leader> leaders(@RequestParam Long taskId){return service.leaders(taskId);}@PostMapping @PreAuthorize("hasRole('REPORTER')") public LateFillDtos.Response create(@Valid @RequestBody LateFillDtos.Create input){return service.create(input);}@PatchMapping("/{id}/approve") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public LateFillDtos.Response approve(@PathVariable Long id,@Valid @RequestBody LateFillDtos.Review input){return service.approve(id,input);}@PatchMapping("/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public LateFillDtos.Response reject(@PathVariable Long id,@RequestParam String comment){return service.reject(id,comment);} }
