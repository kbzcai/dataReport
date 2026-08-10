package com.sjtb.reporting.service;

import com.sjtb.reporting.domain.ReportTaskSchedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

final class TaskScheduleTiming {
    private TaskScheduleTiming() { }

    static LocalDateTime nextRun(ReportTaskSchedule schedule, LocalDateTime from) {
        LocalDate date = from.toLocalDate();
        LocalTime time = schedule.getPublishTime();
        LocalDateTime candidate;
        if ("WEEKLY".equals(schedule.getFrequency())) {
            candidate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(schedule.getWeekDay()))).atTime(time);
            if (!candidate.isAfter(from)) candidate = candidate.plusWeeks(1);
        } else if ("MONTHLY".equals(schedule.getFrequency())) {
            candidate = atDay(date.getYear(), date.getMonthValue(), schedule.getDayOfMonth(), time);
            if (!candidate.isAfter(from)) {
                LocalDate nextMonth = date.plusMonths(1);
                candidate = atDay(nextMonth.getYear(), nextMonth.getMonthValue(), schedule.getDayOfMonth(), time);
            }
        } else {
            candidate = atDay(date.getYear(), schedule.getMonthOfYear(), schedule.getDayOfMonth(), time);
            if (!candidate.isAfter(from)) candidate = atDay(date.getYear() + 1, schedule.getMonthOfYear(), schedule.getDayOfMonth(), time);
        }
        if (schedule.getStartAt() != null && candidate.isBefore(schedule.getStartAt())) {
            return nextRun(schedule, schedule.getStartAt().minusSeconds(1));
        }
        return candidate;
    }

    static String periodKey(ReportTaskSchedule schedule, LocalDateTime at) {
        return switch (schedule.getFrequency()) {
            case "WEEKLY" -> at.get(WeekFields.ISO.weekBasedYear()) + "-W" + String.format("%02d", at.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case "MONTHLY" -> String.format("%04d-%02d", at.getYear(), at.getMonthValue());
            default -> String.valueOf(at.getYear());
        };
    }

    private static LocalDateTime atDay(int year, int month, int configuredDay, LocalTime time) {
        YearMonth value = YearMonth.of(year, month);
        int day = configuredDay == 0 ? value.lengthOfMonth() : Math.min(configuredDay, value.lengthOfMonth());
        return value.atDay(day).atTime(time);
    }
}
