package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.TaskDtos;
import com.sjtb.reporting.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','LEADER','REPORTER')") public List<TaskDtos.Response> list() { return tasks.list(); }
    @GetMapping("/reminders") @PreAuthorize("hasRole('REPORTER')") public List<TaskDtos.Reminder> reminders() { return tasks.reminders(); }
    @GetMapping("/overview") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public TaskDtos.Overview overview() { return tasks.overview(); }
    @GetMapping("/assignable-targets") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public List<TaskDtos.AssignableTarget> assignableTargets() { return tasks.assignableTargets(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public TaskDtos.Response create(@Valid @RequestBody TaskDtos.Request request) { return tasks.create(request); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public TaskDtos.Response update(@PathVariable Long id, @Valid @RequestBody TaskDtos.Request request) { return tasks.update(id, request); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','LEADER')") public void delete(@PathVariable Long id) { tasks.delete(id); }
}
