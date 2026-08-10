package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.ScheduleDtos;
import com.sjtb.reporting.service.TaskScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/task-schedules")
@PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
public class TaskScheduleController {
    private final TaskScheduleService schedules;
    public TaskScheduleController(TaskScheduleService schedules) { this.schedules = schedules; }
    @GetMapping public List<ScheduleDtos.Response> list() { return schedules.list(); }
    @GetMapping("/targets") public List<ScheduleDtos.Target> targets() { return schedules.targets(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ScheduleDtos.Response create(@Valid @RequestBody ScheduleDtos.Request request) { return schedules.create(request); }
    @PutMapping("/{id}") public ScheduleDtos.Response update(@PathVariable Long id, @Valid @RequestBody ScheduleDtos.Request request) { return schedules.update(id, request); }
    @PostMapping("/{id}/pause") public ScheduleDtos.Response pause(@PathVariable Long id) { return schedules.pause(id); }
    @PostMapping("/{id}/resume") public ScheduleDtos.Response resume(@PathVariable Long id) { return schedules.resume(id); }
    @PostMapping("/{id}/run-now") public ScheduleDtos.RunResponse runNow(@PathVariable Long id) { return schedules.runNow(id); }
    @GetMapping("/{id}/runs") public List<ScheduleDtos.RunResponse> runs(@PathVariable Long id) { return schedules.runs(id); }
}
