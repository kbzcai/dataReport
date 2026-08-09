import http from './http'

export interface Department { id: number | string; name: string; parentId?: number | string | null }
export const listDepartments = () => http.get<unknown, Department[]>('/departments')
export const createDepartment = (data: { name: string; parentId?: number | string | null }) => http.post<unknown, Department>('/departments', data)
export const updateDepartment = (id: Department['id'], data: { name: string; parentId?: number | string | null }) => http.put<unknown, Department>(`/departments/${id}`, data)
export const deleteDepartment = (id: Department['id']) => http.delete<unknown, void>(`/departments/${id}`)
