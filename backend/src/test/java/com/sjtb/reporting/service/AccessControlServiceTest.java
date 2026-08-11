package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.domain.Permission;
import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.Role;
import com.sjtb.reporting.domain.User;
import com.sjtb.reporting.repository.DepartmentRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AccessControlServiceTest {
    @Test
    void leaderCanOnlyReadRecordsFromOwnDepartmentTreeAndNeedsViewPermission() {
        DepartmentRepository departments = org.mockito.Mockito.mock(DepartmentRepository.class);
        Department root = department(1L, null);
        Department child = department(2L, root);
        Department other = department(3L, null);
        when(departments.findAll()).thenReturn(List.of(root, child, other));
        AccessControlService access = new AccessControlService(departments);

        User leader = user(10L, Set.of(Role.LEADER), Set.of(Permission.REPORT_VIEW), root);
        User childReporter = user(11L, Set.of(Role.REPORTER), Set.of(Permission.REPORT_VIEW, Permission.REPORT_EDIT), child);
        User otherReporter = user(12L, Set.of(Role.REPORTER), Set.of(Permission.REPORT_VIEW, Permission.REPORT_EDIT), other);

        assertThat(access.canReadRecord(leader, record(childReporter))).isTrue();
        assertThat(access.canReadRecord(leader, record(otherReporter))).isFalse();
        leader.setPermissions(Set.of());
        assertThat(access.canReadRecord(leader, record(childReporter))).isFalse();
    }

    @Test
    void assigneeMustBeReporterAndHaveEditPermission() {
        AccessControlService access = new AccessControlService(org.mockito.Mockito.mock(DepartmentRepository.class));
        User eligible = user(1L, Set.of(Role.REPORTER), Set.of(Permission.REPORT_EDIT), null);
        User viewOnly = user(2L, Set.of(Role.REPORTER), Set.of(Permission.REPORT_VIEW), null);

        assertThat(access.isEligibleAssignee(eligible)).isTrue();
        assertThat(access.isEligibleAssignee(viewOnly)).isFalse();
    }

    @Test
    void leaderCannotEditAReportEvenInsideTheirDepartmentTree() {
        DepartmentRepository departments = org.mockito.Mockito.mock(DepartmentRepository.class);
        Department root = department(1L, null);
        Department child = department(2L, root);
        when(departments.findAll()).thenReturn(List.of(root, child));
        AccessControlService access = new AccessControlService(departments);

        User leader = user(10L, Set.of(Role.LEADER), Set.of(Permission.REPORT_VIEW, Permission.REPORT_EDIT), root);
        User reporter = user(11L, Set.of(Role.REPORTER), Set.of(Permission.REPORT_VIEW, Permission.REPORT_EDIT), child);

        assertThat(access.canEditRecord(leader, record(reporter))).isFalse();
    }

    private static ReportRecord record(User reporter) {
        ReportRecord record = new ReportRecord();
        record.setReporter(reporter);
        return record;
    }

    private static Department department(Long id, Department parent) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setParent(parent);
        return department;
    }

    private static User user(Long id, Set<Role> roles, Set<Permission> permissions, Department department) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setDepartment(department);
        return user;
    }
}
