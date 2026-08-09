package com.sjtb.reporting.dto;

import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public final class UserDtos {
    private UserDtos() { }
    public record CreateRequest(@NotBlank String username, @NotBlank String password, @NotEmpty Set<Role> roles, Set<Permission> permissions, Long departmentId) { }
    public record UpdateRequest(String password, Set<Role> roles, Set<Permission> permissions, Boolean enabled, Long departmentId, Boolean departmentProvided) { }
    public record Response(Long id, String username, Set<Role> roles, Set<Permission> permissions, Long departmentId, String departmentName, boolean enabled) { }
}
