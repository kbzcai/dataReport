package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportRecord;
import com.sjtb.reporting.domain.ReportStatus;
import java.util.List;
import java.util.Set;
import com.sjtb.reporting.dto.ReportDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {
    List<ReportRecord> findByReporterIdOrderByUpdatedAtDesc(Long reporterId);
    List<ReportRecord> findByTemplateIdOrderByUpdatedAtDesc(Long templateId);
    List<ReportRecord> findByTemplateId(Long templateId);
    Page<ReportRecord> findByTemplateId(Long templateId, Pageable pageable);
    Page<ReportRecord> findByTemplateIdAndReporterId(Long templateId, Long reporterId, Pageable pageable);
    Page<ReportRecord> findByReporterId(Long reporterId, Pageable pageable);
    Page<ReportRecord> findByReporterIdAndTemplateId(Long reporterId, Long templateId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select record from ReportRecord record where record.id = :id")
    Optional<ReportRecord> findByIdForUpdate(@Param("id") Long id);
    boolean existsByTaskId(Long taskId);
    boolean existsByTaskIdAndReporterIdAndStatusIn(Long taskId, Long reporterId, Set<ReportStatus> statuses);
    @Query("select new com.sjtb.reporting.dto.TaskDtos$ProgressAggregation(record.task.id, count(distinct record.reporter.id)) from ReportRecord record join record.task task join task.assignees assignee where record.task.id in :taskIds and record.reporter.id = assignee.id and record.status in :statuses group by record.task.id")
    List<com.sjtb.reporting.dto.TaskDtos.ProgressAggregation> findTaskProgressByTaskIds(@Param("taskIds") List<Long> taskIds, @Param("statuses") Set<ReportStatus> statuses);
    @Query("select new com.sjtb.reporting.dto.TaskDtos$DetailProgressAggregation(record.task.id, count(record.id), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.DRAFT then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.SUBMITTED then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.RETURNED then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.APPROVED then 1 else 0 end), max(record.updatedAt)) from ReportRecord record where record.task.id in :taskIds and record.status in :statuses group by record.task.id")
    List<com.sjtb.reporting.dto.TaskDtos.DetailProgressAggregation> findTaskDetailProgressByTaskIds(@Param("taskIds") List<Long> taskIds, @Param("statuses") Set<ReportStatus> statuses);
    @Query("select new com.sjtb.reporting.dto.TaskDtos$DetailProgressAggregation(record.task.id, count(record.id), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.DRAFT then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.SUBMITTED then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.RETURNED then 1 else 0 end), sum(case when record.status = com.sjtb.reporting.domain.ReportStatus.APPROVED then 1 else 0 end), max(record.updatedAt)) from ReportRecord record where record.task.id in :taskIds and record.reporter.id = :reporterId and record.status in :statuses group by record.task.id")
    List<com.sjtb.reporting.dto.TaskDtos.DetailProgressAggregation> findTaskDetailProgressByTaskIdsAndReporterId(@Param("taskIds") List<Long> taskIds, @Param("reporterId") Long reporterId, @Param("statuses") Set<ReportStatus> statuses);
    @Query("select new com.sjtb.reporting.dto.ReportDtos$Summary(record.template.id, record.template.name, record.reporter.id, record.reporter.username, count(record.id)) from ReportRecord record group by record.template.id, record.template.name, record.reporter.id, record.reporter.username")
    List<ReportDtos.Summary> findSummaries();
    @Query("select new com.sjtb.reporting.dto.ReportDtos$Summary(record.template.id, record.template.name, record.reporter.id, record.reporter.username, count(record.id)) from ReportRecord record where record.reporter.id = :reporterId group by record.template.id, record.template.name, record.reporter.id, record.reporter.username")
    List<ReportDtos.Summary> findSummariesByReporterId(@Param("reporterId") Long reporterId);
    @Modifying @Query("delete from ReportRecord record where record.template.id = :templateId")
    void deleteAllByTemplateId(@Param("templateId") Long templateId);
}
