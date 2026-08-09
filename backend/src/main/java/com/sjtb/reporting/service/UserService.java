package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.dto.UserDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.UserRepository;
import com.sjtb.reporting.repository.DepartmentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class UserService {
    private final UserRepository users; private final DepartmentRepository departments; private final PasswordEncoder encoder;
    public UserService(UserRepository users, DepartmentRepository departments, PasswordEncoder encoder) { this.users = users; this.departments = departments; this.encoder = encoder; }
    public List<UserDtos.Response> list() { return users.findAll().stream().map(this::toResponse).toList(); }
    public UserDtos.Response create(UserDtos.CreateRequest request) {
        if (users.existsByUsername(request.username())) throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        User user = new User(); user.setUsername(request.username()); user.setPassword(encoder.encode(request.password())); user.setRoles(request.roles()); user.setPermissions(request.permissions() == null ? java.util.Set.of() : request.permissions()); user.setDepartment(department(request.departmentId())); return toResponse(users.save(user));
    }
    public UserDtos.Response update(Long id, UserDtos.UpdateRequest request) {
        User user = find(id); if (request.password() != null && !request.password().isBlank()) user.setPassword(encoder.encode(request.password()));
        if (request.roles() != null && !request.roles().isEmpty()) user.setRoles(request.roles()); if (request.permissions() != null) user.setPermissions(request.permissions()); if (Boolean.TRUE.equals(request.departmentProvided())) user.setDepartment(department(request.departmentId())); if (request.enabled() != null) user.setEnabled(request.enabled()); return toResponse(user);
    }
    public void delete(Long id) { users.delete(find(id)); }
    private User find(Long id) { return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found")); }
    private Department department(Long id) { return id == null ? null : departments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Department not found")); }
    private UserDtos.Response toResponse(User user) { return new UserDtos.Response(user.getId(), user.getUsername(), user.getRoles(), user.getPermissions(), user.getDepartment() == null ? null : user.getDepartment().getId(), user.getDepartment() == null ? null : user.getDepartment().getName(), user.isEnabled()); }
}
