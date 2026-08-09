package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.Permission;
import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Centralizes role, permission, and department-tree checks for report data. */
@Service
public class AccessControlService {
    private final DepartmentRepository departments;

    public AccessControlService(DepartmentRepository departments) {
        this.departments = departments;
    }

    public boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN);
    }

    public boolean isLeader(User user) {
        return user.getRoles().contains(Role.LEADER);
    }

    public boolean hasView(User user) {
        return isAdmin(user) || user.getPermissions().contains(Permission.REPORT_VIEW);
    }

    public boolean hasEdit(User user) {
        return isAdmin(user) || user.getPermissions().contains(Permission.REPORT_EDIT);
    }

    public void requireView(User user) {
        if (!hasView(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_VIEW permission is required");
        }
    }

    public void requireEdit(User user) {
        if (!hasEdit(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_EDIT permission is required");
        }
    }

    public Set<Long> scopeDepartmentIds(User user) {
        if (isAdmin(user)) {
            return Set.of();
        }
        if (!isLeader(user) || user.getDepartment() == null) {
            return Set.of();
        }
        Long rootId = user.getDepartment().getId();
        Set<Long> scope = new HashSet<>();
        scope.add(rootId);
        boolean changed;
        do {
            changed = false;
            for (Department department : departments.findAll()) {
                Department parent = department.getParent();
                if (parent != null && scope.contains(parent.getId())) {
                    changed |= scope.add(department.getId());
                }
            }
        } while (changed);
        return scope;
    }

    public boolean canManageDepartment(User actor, Department target) {
        return canManageDepartment(actor, target, scopeDepartmentIds(actor));
    }

    public boolean canManageDepartment(User actor, Department target, Set<Long> scope) {
        return isAdmin(actor) || (isLeader(actor) && target != null && scope.contains(target.getId()));
    }

    public boolean canReadRecord(User actor, ReportRecord record) {
        return canReadRecord(actor, record, scopeDepartmentIds(actor));
    }

    public boolean canReadRecord(User actor, ReportRecord record, Set<Long> scope) {
        if (!hasView(actor)) {
            return false;
        }
        return isAdmin(actor)
                || record.getReporter().getId().equals(actor.getId())
                || canManageDepartment(actor, record.getReporter().getDepartment(), scope);
    }

    public boolean canEditRecord(User actor, ReportRecord record) {
        return canEditRecord(actor, record, scopeDepartmentIds(actor));
    }

    public boolean canEditRecord(User actor, ReportRecord record, Set<Long> scope) {
        if (!hasEdit(actor)) {
            return false;
        }
        return isAdmin(actor)
                || record.getReporter().getId().equals(actor.getId())
                || canManageDepartment(actor, record.getReporter().getDepartment(), scope);
    }

    public boolean isEligibleAssignee(User user) {
        return user.isEnabled()
                && user.getRoles().contains(Role.REPORTER)
                && hasEdit(user);
    }
}
