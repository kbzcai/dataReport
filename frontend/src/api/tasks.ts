import http from './http'
import type { ReportTask } from '../types'

export const listTasks = () => http.get<unknown, ReportTask[]>('/tasks')
export const listAssignableUsers = () => http.get<unknown, Array<{ id: number | string; username: string; roles: string[] }>>('/tasks/assignable-users')
export interface TaskReminder { taskId: number | string; taskName: string; templateName: string; periodLabel?: string; deadline?: string; level: 'PENDING' | 'DUE_SOON' | 'OVERDUE' }
export interface TaskOverview { total: number; published: number; dueSoon: number; overdue: number; completed: number; pendingAssignees: number }
export const listTaskReminders = () => http.get<unknown, TaskReminder[]>('/tasks/reminders')
export const getTaskOverview = () => http.get<unknown, TaskOverview>('/tasks/overview')
export const createTask = (data: Omit<ReportTask, 'id' | 'templateName'>) => http.post<unknown, ReportTask>('/tasks', data)
export const updateTask = (id: ReportTask['id'], data: Omit<ReportTask, 'id' | 'templateName'>) => http.put<unknown, ReportTask>(`/tasks/${id}`, data)
export const deleteTask = (id: ReportTask['id']) => http.delete<unknown, void>(`/tasks/${id}`)
