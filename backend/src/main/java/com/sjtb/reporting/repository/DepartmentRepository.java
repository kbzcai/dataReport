package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByNameAsc();
    boolean existsByParentId(Long parentId);
}
