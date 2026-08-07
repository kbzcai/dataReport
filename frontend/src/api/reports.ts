import http from './http'
import type { Report } from '../types'

export interface ReportQuery { templateId?: string; keyword?: string; page?: number; size?: number }
export interface ReportSummary { templateId: number | string; templateName: string; reporterId: number | string; reporterName: string; recordCount: number }
export interface ReportPage { records: Report[]; total: number; page: number; size: number; totalPages: number }
export interface ImportSheetPreview { sheetOrder: number; sheetIndex: number; sheetName: string; suggestedTemplateId?: number | string; suggestedTemplateName?: string; matchStatus: 'NAME' | 'HEADER' | 'UNMATCHED' | 'AMBIGUOUS' }
export interface ImportResult { batchId: number | string; importedRows: number; failedRows: number; status: string }
export interface ImportBatch { id: number | string; originalFileName: string; creatorName: string; status: string; importedRows: number; failedRows: number; summary?: string; createdAt?: string; completedAt?: string }
export interface ReportChangeLog { id: number | string; actorName: string; action: string; reason?: string; createdAt?: string }
export const importPreview = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return http.post<unknown, { sheets: ImportSheetPreview[] }>('/reports/import-preview', form)
}
export const confirmImport = (file: File, templateIds: Array<number | string>) => {
  const form = new FormData()
  form.append('file', file)
  form.append('mapping', JSON.stringify(templateIds.map((id) => Number(id))))
  return http.post<unknown, ImportResult>('/reports/import-confirm', form)
}
export const importReport = (templateId: string | number | undefined, file: File, taskId?: string | number) => {
  const form = new FormData()
  // 自动匹配模式不携带 templateId，由后端按工作表识别模板。
  if (templateId !== undefined && String(templateId).trim()) form.append('templateId', String(templateId))
  if (taskId !== undefined && String(taskId).trim()) form.append('taskId', String(taskId))
  form.append('file', file)
  return http.post<unknown, ImportResult>('/reports/import', form)
}
export const listImportBatches = () => http.get<unknown, ImportBatch[]>('/reports/import-batches')
export const downloadImportErrors = async (id: number | string) => {
  const response = await http.get('/reports/import-batches/' + id + '/errors/download', { responseType: 'blob' })
  const url = URL.createObjectURL(response as unknown as Blob); const link = document.createElement('a'); link.href = url; link.download = `导入错误-${id}.csv`; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url)
}
export const listChangeLogs = (id: number | string) => http.get<unknown, ReportChangeLog[]>(`/reports/${id}/change-logs`)
export const createReport = (data: { templateId: number | string; data: Record<string, unknown>; status?: string }) => http.post<unknown, Report>('/reports', data)
export const createReports = (data: Array<{ templateId: number | string; taskId?: number | string; data: Record<string, unknown>; status?: string }>) => http.post<unknown, Report[]>('/reports/batch', data)
export const listReports = (params: ReportQuery) => http.get<unknown, Report[] | { records: Report[] }>('/reports', { params })
export const listReportSummaries = () => http.get<unknown, ReportSummary[]>('/reports/summary')
export const pageReports = (params: { templateId?: number | string; reporterId?: number | string; page: number; size: number }) => http.get<unknown, ReportPage>('/reports/page', { params })
export const updateReport = (id: Report['id'], data: Partial<Report>) => http.put<unknown, Report>(`/reports/${id}`, data)
export const deleteReport = (id: Report['id']) => http.delete<unknown, void>(`/reports/${id}`)
export const exportReports = async (templateId: number | string, taskId?: number | string) => {
  const response = await http.get('/reports/export', { params: { templateId, ...(taskId ? { taskId } : {}) }, responseType: 'blob' })
  const url = URL.createObjectURL(response as unknown as Blob); const link = document.createElement('a'); link.href = url; link.download = '填报数据导出.xlsx'; document.body.appendChild(link); link.click(); link.remove(); URL.revokeObjectURL(url)
}
