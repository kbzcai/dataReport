import http from './http'
import type { User } from '../types'

export interface ManagedUser extends User { roles?: string[]; permissions?: string[]; enabled: boolean }
export const listUsers = () => http.get<unknown, ManagedUser[]>('/users')
export const createUser = (data: { username: string; password: string; roles: string[]; permissions?: string[]; departmentId?: number | string | null }) => http.post<unknown, ManagedUser>('/users', data)
export const updateUser = (id: number | string, data: { password?: string; roles?: string[]; permissions?: string[]; enabled?: boolean; departmentId?: number | string | null; departmentProvided?: boolean }) => http.put<unknown, ManagedUser>(`/users/${id}`, data)
export const deleteUser = (id: number | string) => http.delete<unknown, void>(`/users/${id}`)
