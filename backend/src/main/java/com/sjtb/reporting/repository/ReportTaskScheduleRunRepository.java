package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTaskScheduleRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTaskScheduleRunRepository extends JpaRepository<ReportTaskScheduleRun, Long> {
    Optional<ReportTaskScheduleRun> findByScheduleIdAndPeriodKey(Long scheduleId, String periodKey);
    List<ReportTaskScheduleRun> findTop20ByScheduleIdOrderByExecutedAtDesc(Long scheduleId);
}
