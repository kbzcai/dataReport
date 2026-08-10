import http from './http'
import type { Template, TemplateVersion } from '../types'

export interface TemplateSheetPreview { sheetOrder: number; sheetIndex: number; sheetName: string; valid: boolean; message: string }

export const listTemplates = () => http.get<unknown, Template[]>('/templates')
export const listTemplateVersions = (id: Template['id']) => http.get<unknown, TemplateVersion[]>(`/templates/${id}/versions`)
export const createTemplate = (data: Pick<Template, 'name' | 'description' | 'columns'>) => http.post<unknown, Template>('/templates', data)
export const updateTemplate = (id: Template['id'], data: Partial<Template>) => http.put<unknown, Template>(`/templates/${id}`, data)
export const deleteTemplate = (id: Template['id']) => http.delete<unknown, void>(`/templates/${id}`)
export const uploadTemplateFile = (id: Template['id'], file: File) => {
  const form = new FormData()
  form.append('file', file)
  return http.post<unknown, Template>(`/templates/${id}/file`, form)
}
export const previewTemplateImport = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return http.post<unknown, { sheets: TemplateSheetPreview[] }>('/templates/import-preview', form)
}
export const confirmTemplateImport = (file: File, names: string[]) => {
  const form = new FormData()
  form.append('file', file)
  form.append('names', JSON.stringify(names))
  return http.post<unknown, Template[]>('/templates/import-confirm', form)
}
async function downloadBlob(path: string, name: string) {
  const blob = await http.get<unknown, Blob>(path, { responseType: 'blob' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const safeName = name.replace(/[\\/:*?"<>|]/g, '_').trim() || 'template-download.xlsx'
  link.download = `${safeName}.xlsx`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
export const downloadTemplate = (id: Template['id'], name = `template-${id}`) => downloadBlob(`/templates/${id}/download`, name)
export const downloadTemplateImportSample = () => downloadBlob('/templates/import-sample', '模板导入样例')
