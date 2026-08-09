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
        var authorities = user.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toSet());
        return new AuthDtos.LoginResponse(jwtService.createToken(user), user.getUsername(), authorities.stream().filter(value -> value.startsWith("ROLE_")).map(value -> value.substring(5)).collect(java.util.stream.Collectors.toSet()), authorities.stream().filter(value -> value.startsWith("PERM_")).map(value -> value.substring(5)).collect(java.util.stream.Collectors.toSet()));
    }
}
