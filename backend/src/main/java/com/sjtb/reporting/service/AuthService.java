package com.sjtb.reporting.service;

import com.sjtb.reporting.dto.AuthDtos;
import com.sjtb.reporting.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager; private final JwtService jwtService;
    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) { this.authenticationManager = authenticationManager; this.jwtService = jwtService; }
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserDetails user = (UserDetails) authentication.getPrincipal();
        return new AuthDtos.LoginResponse(jwtService.createToken(user), user.getUsername(), user.getAuthorities().stream().map(a -> a.getAuthority().replace("ROLE_", "")).collect(java.util.stream.Collectors.toSet()));
    }
}
