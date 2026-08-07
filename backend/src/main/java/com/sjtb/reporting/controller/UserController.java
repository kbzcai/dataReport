package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.UserDtos;
import com.sjtb.reporting.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/users") @PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users = users; }
    @GetMapping public List<UserDtos.Response> list() { return users.list(); }
    @PostMapping public UserDtos.Response create(@Valid @RequestBody UserDtos.CreateRequest request) { return users.create(request); }
    @PutMapping("/{id}") public UserDtos.Response update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateRequest request) { return users.update(id, request); }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { users.delete(id); }
}
