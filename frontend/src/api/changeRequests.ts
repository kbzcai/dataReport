import http from './http'

export interface ChangeRequest { id: number | string; reportId: number | string; taskId?: number | string; taskName?: string; templateId: number | string; templateName?: string; requesterName?: string; proposedData: Record<string, unknown>; reason: string; baseUpdatedAt: string; status: string; reviewerName?: string; reviewComment?: string; createdAt?: string }
export const listChangeRequests = (status?: string) => http.get<unknown, ChangeRequest[]>('/change-requests', { params: status ? { status } : undefined })
export const createChangeRequest = (reportId: number | string, data: { data: Record<string, unknown>; reason: string; baseUpdatedAt: string }) => http.post<unknown, ChangeRequest>(`/reports/${reportId}/change-requests`, data)
export const approveChangeRequest = (id: number | string, reviewComment?: string) => http.patch<unknown, ChangeRequest>(`/change-requests/${id}/approve`, { reviewComment })
export const rejectChangeRequest = (id: number | string, reviewComment: string) => http.patch<unknown, ChangeRequest>(`/change-requests/${id}/reject`, { reviewComment })
export const cancelChangeRequest = (id: number | string) => http.patch<unknown, ChangeRequest>(`/change-requests/${id}/cancel`)
