package com.sjtb.reporting.repository;

import com.sjtb.reporting.domain.ReportTaskSchedule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTaskScheduleRepository extends JpaRepository<ReportTaskSchedule, Long> {
    List<ReportTaskSchedule> findAllByOrderByNextRunAtAsc();
    List<ReportTaskSchedule> findByStatusAndNextRunAtLessThanEqual(String status, LocalDateTime now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReportTaskSchedule> findWithLockById(Long id);
}
