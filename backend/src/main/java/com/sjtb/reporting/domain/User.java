package com.sjtb.reporting.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sys_user")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String username;
    @Column(nullable = false) private String password;
    @Column(nullable = false) private boolean enabled = true;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "sys_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable = false) private Set<Role> roles = new HashSet<>();
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "sys_user_permission", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) @Column(name = "permission", nullable = false) private Set<Permission> permissions = new HashSet<>();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId() { return id; } public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<Role> getRoles() { return roles; } public void setRoles(Set<Role> roles) { this.roles = roles; }
    public Set<Permission> getPermissions() { return permissions; } public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }
    public Department getDepartment() { return department; } public void setDepartment(Department department) { this.department = department; }
}
