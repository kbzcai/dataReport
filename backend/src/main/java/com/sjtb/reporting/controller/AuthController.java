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
        String role = authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        return Map.of("username", authentication.getName(), "role", role);
    }
}
