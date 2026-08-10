import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import AppLayout from '../views/AppLayout.vue'
import TemplateView from '../views/TemplateView.vue'
import ReportImportView from '../views/ReportImportView.vue'
import ReportManageView from '../views/ReportManageView.vue'
import UserManageView from '../views/UserManageView.vue'
import TaskView from '../views/TaskView.vue'
import MyReportsView from '../views/MyReportsView.vue'
import ApprovalView from '../views/ApprovalView.vue'
import ReminderView from '../views/ReminderView.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import TaskScheduleView from '../views/TaskScheduleView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    {
      path: '/', component: AppLayout, children: [
        { path: '', redirect: '/reports/import' },
        { path: 'templates', component: TemplateView, meta: { roles: ['MAINTAINER', 'ADMIN'] } },
        { path: 'task-schedules', component: TaskScheduleView, meta: { roles: ['MAINTAINER', 'ADMIN'] } },
        { path: 'users', component: UserManageView, meta: { roles: ['ADMIN'] } },
        { path: 'tasks', component: TaskView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'my-reports', component: MyReportsView, meta: { roles: ['REPORTER'] } },
        { path: 'reminders', component: ReminderView, meta: { roles: ['REPORTER'] } },
        { path: 'approvals', component: ApprovalView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'analytics', component: AnalyticsView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'departments', component: () => import('../views/DepartmentManageView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'reports/import', component: ReportImportView, meta: { roles: ['REPORTER', 'LEADER', 'ADMIN'], permissions: ['REPORT_EDIT'] } },
        { path: 'reports', component: ReportManageView, meta: { roles: ['LEADER', 'ADMIN'] } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

function defaultPath(roles: string[], permissions: string[]) {
  if (roles.includes('ADMIN') || roles.includes('LEADER')) return '/reports'
  if (roles.includes('MAINTAINER')) return '/templates'
  if (roles.includes('REPORTER') && permissions.includes('REPORT_EDIT')) return '/reports/import'
  if (roles.includes('REPORTER')) return '/my-reports'
  return '/login'
}

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.loadUser()
  if (to.name === 'login' && !auth.loggedIn) return true
  if (to.meta.public) return auth.loggedIn ? defaultPath(auth.roles, auth.permissions) : true
  if (!auth.loggedIn) return '/login'
  const roles = to.meta.roles as string[] | undefined
  if (roles?.length && !roles.some(role => auth.roles.includes(role))) return defaultPath(auth.roles, auth.permissions)
  const permissions = to.meta.permissions as string[] | undefined
  if (permissions?.length && !permissions.some(permission => auth.permissions.includes(permission))) return defaultPath(auth.roles, auth.permissions)
  return true
})

export default router
