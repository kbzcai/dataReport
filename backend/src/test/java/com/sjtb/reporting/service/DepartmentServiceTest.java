package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.dto.DepartmentDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

class DepartmentServiceTest {
    @Test
    void updateRejectsUsingDescendantAsParent() {
        DepartmentRepository departments = org.mockito.Mockito.mock(DepartmentRepository.class);
        Department root = department(1L, null);
        Department child = department(2L, root);
        Department grandchild = department(3L, child);
        when(departments.findById(1L)).thenReturn(Optional.of(root));
        when(departments.findById(3L)).thenReturn(Optional.of(grandchild));
        DepartmentService service = new DepartmentService(departments, org.mockito.Mockito.mock(UserRepository.class), org.mockito.Mockito.mock(ReportTaskRepository.class));

        assertThatThrownBy(() -> service.update(1L, new DepartmentDtos.Request("Root", 3L)))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("descendants");
    }

    private static Department department(Long id, Department parent) {
        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", id);
        department.setParent(parent);
        return department;
    }
}
