package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }
    public User current() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByUsername(username).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User session is invalid"));
    }
    public boolean hasAny(Role... roles) { for (Role role : roles) if (current().getRoles().contains(role)) return true; return false; }
}
