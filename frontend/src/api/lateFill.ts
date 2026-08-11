import http from './http'
export interface LateFillRequest { id: number|string; taskId: number|string; taskName?: string; requesterName?: string; leaderId: number|string; leaderName?: string; reason: string; status: string; lateDeadline?: string; reviewComment?: string; createdAt?: string }
export const listLateFillRequests = () => http.get<unknown, LateFillRequest[]>('/late-fill-requests')
export const listLateFillLeaders = (taskId: number|string) => http.get<unknown, Array<{id:number|string;username:string}>>('/late-fill-requests/leaders',{params:{taskId}})
export const createLateFillRequest = (data:{taskId:number|string;leaderId:number|string;reason:string}) => http.post<unknown,LateFillRequest>('/late-fill-requests',data)
export const approveLateFillRequest = (id:number|string, lateDeadline:string, reviewComment?:string) => http.patch<unknown,LateFillRequest>(`/late-fill-requests/${id}/approve`,{lateDeadline,reviewComment})
export const rejectLateFillRequest = (id:number|string, comment:string) => http.patch<unknown,LateFillRequest>(`/late-fill-requests/${id}/reject`,undefined,{params:{comment}})
