import http from './http'
import type { TaskSchedule } from '../types'

export interface ScheduleTarget { id: number | string; name: string; type: 'DEPARTMENT' | 'REPORTER' }
export interface ScheduleRun { id: number | string; periodKey: string; taskId?: number | string; status: string; errorMessage?: string; executedAt?: string }
export const listTaskSchedules = () => http.get<unknown, TaskSchedule[]>('/task-schedules')
export const listScheduleTargets = () => http.get<unknown, ScheduleTarget[]>('/task-schedules/targets')
export const createTaskSchedule = (data: Omit<TaskSchedule, 'id' | 'templateName' | 'templateVersionNo' | 'status' | 'nextRunAt'>) => http.post<unknown, TaskSchedule>('/task-schedules', data)
export const updateTaskSchedule = (id: number | string, data: Omit<TaskSchedule, 'id' | 'templateName' | 'templateVersionNo' | 'status' | 'nextRunAt'>) => http.put<unknown, TaskSchedule>(`/task-schedules/${id}`, data)
export const pauseTaskSchedule = (id: number | string) => http.post<unknown, TaskSchedule>(`/task-schedules/${id}/pause`)
export const resumeTaskSchedule = (id: number | string) => http.post<unknown, TaskSchedule>(`/task-schedules/${id}/resume`)
export const runTaskScheduleNow = (id: number | string) => http.post<unknown, ScheduleRun>(`/task-schedules/${id}/run-now`)
export const listTaskScheduleRuns = (id: number | string) => http.get<unknown, ScheduleRun[]>(`/task-schedules/${id}/runs`)
