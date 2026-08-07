package com.sjtb.reporting.dto;

import com.sjtb.reporting.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public final class UserDtos {
    private UserDtos() { }
    public record CreateRequest(@NotBlank String username, @NotBlank String password, @NotEmpty Set<Role> roles) { }
    public record UpdateRequest(String password, Set<Role> roles, Boolean enabled) { }
    public record Response(Long id, String username, Set<Role> roles, boolean enabled) { }
}
