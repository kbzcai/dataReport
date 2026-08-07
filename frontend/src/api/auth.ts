import http from './http'
import type { User } from '../types'

export interface LoginResult { token?: string; accessToken?: string; user?: User }

export const login = (username: string, password: string) =>
  http.post<unknown, LoginResult>('/auth/login', { username, password })
export const getCurrentUser = () => http.get<unknown, User>('/auth/me')
