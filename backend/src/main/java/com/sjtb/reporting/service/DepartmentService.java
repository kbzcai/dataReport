package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.Department;
import com.sjtb.reporting.dto.DepartmentDtos;
import com.sjtb.reporting.exception.ApiException;
import com.sjtb.reporting.repository.DepartmentRepository;
import com.sjtb.reporting.repository.ReportTaskRepository;
import com.sjtb.reporting.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class DepartmentService {
    private final DepartmentRepository departments; private final UserRepository users; private final ReportTaskRepository tasks;
    public DepartmentService(DepartmentRepository departments, UserRepository users, ReportTaskRepository tasks) { this.departments = departments; this.users = users; this.tasks = tasks; }
    @Transactional(readOnly = true) public List<DepartmentDtos.Response> list() { return departments.findAllByOrderByNameAsc().stream().map(this::response).toList(); }
    public DepartmentDtos.Response create(DepartmentDtos.Request request) { Department department = new Department(); department.setName(request.name().trim()); department.setParent(parent(request.parentId())); return response(departments.save(department)); }
    public DepartmentDtos.Response update(Long id, DepartmentDtos.Request request) {
        Department department = find(id);
        if (id.equals(request.parentId())) throw new ApiException(HttpStatus.BAD_REQUEST, "Department cannot be its own parent");
        Department proposedParent = parent(request.parentId());
        assertParentDoesNotCreateCycle(department, proposedParent);
        department.setName(request.name().trim());
        department.setParent(proposedParent);
        return response(department);
    }
    public void delete(Long id) {
        Department department = find(id);
        if (departments.existsByParentId(id)) throw new ApiException(HttpStatus.CONFLICT, "Department has child departments");
        if (users.existsByDepartmentId(id)) throw new ApiException(HttpStatus.CONFLICT, "Department still has users");
        if (tasks.existsByTargetDepartmentsId(id)) throw new ApiException(HttpStatus.CONFLICT, "Department is used by report tasks");
        departments.delete(department);
    }
    private Department find(Long id) { return departments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found")); }
    private Department parent(Long id) { return id == null ? null : find(id); }
    private void assertParentDoesNotCreateCycle(Department department, Department proposedParent) {
        java.util.Set<Long> visited = new java.util.HashSet<>();
        Department current = proposedParent;
        while (current != null) {
            Long currentId = current.getId();
            if (department.getId().equals(currentId)) throw new ApiException(HttpStatus.BAD_REQUEST, "Department cannot be assigned to one of its descendants");
            if (currentId != null && !visited.add(currentId)) throw new ApiException(HttpStatus.CONFLICT, "Department hierarchy contains a cycle");
            current = current.getParent();
        }
    }
    private DepartmentDtos.Response response(Department department) { return new DepartmentDtos.Response(department.getId(), department.getName(), department.getParent() == null ? null : department.getParent().getId()); }
}
