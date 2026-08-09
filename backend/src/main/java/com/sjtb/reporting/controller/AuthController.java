package com.sjtb.reporting.controller;

import com.sjtb.reporting.dto.AuthDtos;
import com.sjtb.reporting.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login") public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) { return auth.login(request); }
    @GetMapping("/me") public Map<String, Object> me(Authentication authentication) {
        var authorities = authentication.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
        java.util.List<String> roles = authorities.stream().filter(value -> value.startsWith("ROLE_")).map(value -> value.substring(5)).sorted().toList();
        String role = roles.isEmpty() ? "" : roles.get(0);
        return Map.of("username", authentication.getName(), "role", role,
                "roles", new java.util.LinkedHashSet<>(roles),
                "permissions", authorities.stream().filter(value -> value.startsWith("PERM_")).map(value -> value.substring(5)).collect(java.util.stream.Collectors.toSet()));
    }
}
