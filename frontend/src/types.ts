export type Role = 'ADMIN' | 'MAINTAINER' | 'LEADER' | 'REPORTER'

export interface User {
  id: number | string
  username: string
  realName?: string
  role: Role | string
}

export interface TemplateColumn {
  key: string
  label: string
  type?: string
  required?: boolean
  defaultValue?: string
  maxLength?: number
  minValue?: number | string
  maxValue?: number | string
  scale?: number
  pattern?: string
  options?: string[]
  uniqueValue?: boolean
  searchable?: boolean
  aggregatable?: boolean
  listVisible?: boolean
  frozen?: boolean
  importable?: boolean
}

export interface Template {
  id: number | string
  name: string
  description?: string
  fileName?: string
  enabled?: boolean
  updatedAt?: string
  createdAt?: string
  columns?: TemplateColumn[]
  status?: string
  currentVersionId?: number | string
  currentVersionNo?: number
}

export interface TemplateVersion {
  id: number | string
  versionNo: number
  status: string
  createdAt?: string
  columns?: TemplateColumn[]
}

export interface Report {
  id: number | string
  templateId: number | string
  templateName?: string
  templateVersionId?: number | string
  templateVersionNo?: number
  reporterId?: number | string
  reporterName?: string
  status?: string
  data?: Record<string, unknown> | string
  remark?: string
  submittedAt?: string
  updatedAt?: string
  taskId?: number | string
  taskName?: string
}

export interface ReportTask {
  id: number | string
  name: string
  templateId: number | string
  templateName?: string
  templateVersionId?: number | string
  templateVersionNo?: number
  frequency: string
  periodLabel?: string
  startAt?: string
  deadline?: string
  allowLate?: boolean
  status: string
  description?: string
  assigneeIds?: Array<number | string>
  assignees?: Array<{ id: number | string; username: string; roles: string[] }>
  progress?: { assigneeCount: number; submittedAssigneeCount: number; pendingAssigneeCount: number }
}
