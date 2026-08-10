package com.sjtb.reporting.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduleDispatcher {
    private final TaskScheduleService schedules;
    public TaskScheduleDispatcher(TaskScheduleService schedules) { this.schedules = schedules; }
    @Scheduled(fixedDelay = 60000)
    public void dispatchDueSchedules() { schedules.executeDue(); }
}
