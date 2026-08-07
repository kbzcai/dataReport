import { defineStore } from 'pinia'
import { getCurrentUser, login } from '../api/auth'
import type { User } from '../types'

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: null as User | null, initialized: false }),
  getters: {
    loggedIn: () => Boolean(localStorage.getItem('reporting_token')),
    role: (state) => String(state.user?.role || '').toUpperCase(),
  },
  actions: {
    async signIn(username: string, password: string) {
      const result = await login(username, password)
      const token = result.token || result.accessToken
      if (!token) throw new Error('登录响应未包含访问令牌')
      localStorage.setItem('reporting_token', token)
      this.user = result.user || await getCurrentUser()
      this.initialized = true
    },
    async loadUser() {
      if (!this.loggedIn) { this.initialized = true; return }
      try {
        this.user = await getCurrentUser()
      } catch {
        // 后端重启或令牌过期时，避免路由守卫因 401 中断而产生空白页。
        localStorage.removeItem('reporting_token')
        this.user = null
      } finally {
        this.initialized = true
      }
    },
    signOut() {
      localStorage.removeItem('reporting_token')
      this.user = null
      this.initialized = true
    },
  },
})
