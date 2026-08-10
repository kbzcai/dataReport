package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sjtb.reporting.domain.ReportTaskSchedule;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TaskScheduleTimingTest {
    @Test
    void monthlyLastDayUsesActualMonthLength() {
        ReportTaskSchedule schedule = schedule("MONTHLY");
        schedule.setDayOfMonth(0);

        assertThat(TaskScheduleTiming.nextRun(schedule, LocalDateTime.of(2027, 2, 1, 8, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 9, 30));
        assertThat(TaskScheduleTiming.nextRun(schedule, LocalDateTime.of(2028, 2, 28, 10, 0)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 30));
    }

    @Test
    void monthlyConfiguredDayFallsBackToMonthEnd() {
        ReportTaskSchedule schedule = schedule("MONTHLY");
        schedule.setDayOfMonth(31);

        assertThat(TaskScheduleTiming.nextRun(schedule, LocalDateTime.of(2027, 4, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2027, 4, 30, 9, 30));
    }

    @Test
    void yearlyLeapDayFallsBackToFebruaryEndAndWeeklyPeriodUsesIsoYear() {
        ReportTaskSchedule yearly = schedule("YEARLY");
        yearly.setMonthOfYear(2); yearly.setDayOfMonth(29);
        assertThat(TaskScheduleTiming.nextRun(yearly, LocalDateTime.of(2027, 1, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 9, 30));

        ReportTaskSchedule weekly = schedule("WEEKLY");
        weekly.setWeekDay(1);
        assertThat(TaskScheduleTiming.periodKey(weekly, LocalDateTime.of(2024, 12, 30, 9, 30))).isEqualTo("2025-W01");
    }

    private static ReportTaskSchedule schedule(String frequency) {
        ReportTaskSchedule schedule = new ReportTaskSchedule();
        schedule.setFrequency(frequency); schedule.setPublishTime(LocalTime.of(9, 30));
        return schedule;
    }
}
