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
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    {
      path: '/', component: AppLayout, children: [
        { path: '', redirect: '/reports/import' },
        { path: 'templates', component: TemplateView, meta: { roles: ['MAINTAINER', 'ADMIN'] } },
        { path: 'users', component: UserManageView, meta: { roles: ['ADMIN'] } },
        { path: 'tasks', component: TaskView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'my-reports', component: MyReportsView, meta: { roles: ['REPORTER'] } },
        { path: 'reminders', component: ReminderView, meta: { roles: ['REPORTER'] } },
        { path: 'approvals', component: ApprovalView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'analytics', component: AnalyticsView, meta: { roles: ['LEADER', 'ADMIN'] } },
        { path: 'reports/import', component: ReportImportView, meta: { roles: ['REPORTER', 'LEADER', 'ADMIN'] } },
        { path: 'reports', component: ReportManageView, meta: { roles: ['LEADER', 'ADMIN'] } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) await auth.loadUser()
  if (to.meta.public) return auth.loggedIn ? '/' : true
  if (!auth.loggedIn) return '/login'
  const roles = to.meta.roles as string[] | undefined
  if (roles?.length && !roles.includes(auth.role)) return auth.role === 'LEADER' ? '/reports' : '/reports/import'
  return true
})

export default router
