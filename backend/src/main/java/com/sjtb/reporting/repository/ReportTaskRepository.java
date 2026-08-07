package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTask;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {
    List<ReportTask> findByTemplateId(Long templateId);
    @EntityGraph(attributePaths = {"template", "assignees"})
    List<ReportTask> findByAssigneesIdOrderByDeadlineAsc(Long userId);
    @EntityGraph(attributePaths = {"template", "assignees"})
    List<ReportTask> findAllByOrderByDeadlineAsc();
}
