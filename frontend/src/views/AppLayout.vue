<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listTaskReminders, type TaskReminder } from '../api/tasks'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const hasAnyRole = (...roles: string[]) => roles.some((role) => auth.roles.includes(role))
const overdueReminders = ref<TaskReminder[]>([])
const showOverdueReminders = ref(false)
const menus = computed(() => [
  ...(hasAnyRole('ADMIN') ? [{ to: '/users', label: '权限管理', icon: 'U' }, { to: '/departments', label: '部门管理', icon: 'O' }] : []),
  ...(hasAnyRole('ADMIN', 'LEADER') ? [{ to: '/tasks', label: '填报任务', icon: 'K' }] : []),
  ...(hasAnyRole('MAINTAINER', 'ADMIN') ? [{ to: '/templates', label: '模板维护', icon: 'T' }] : []),
  ...(hasAnyRole('MAINTAINER', 'ADMIN') ? [{ to: '/task-schedules', label: '定时发布', icon: 'S' }] : []),
  ...(hasAnyRole('REPORTER', 'ADMIN') && auth.hasPermission('REPORT_EDIT') ? [{ to: '/reports/import', label: '数据填报', icon: 'F' }] : []),
  ...(hasAnyRole('REPORTER') ? [{ to: '/my-reports', label: '我的填报', icon: 'M' }, { to: '/reminders', label: '填报提醒', icon: 'R' }] : []),
  ...(hasAnyRole('REPORTER', 'LEADER', 'ADMIN') ? [{ to: '/late-fill-requests', label: '补报申请', icon: 'L' }] : []),
  ...(hasAnyRole('LEADER', 'ADMIN') ? [{ to: '/reports', label: '填报数据', icon: 'D' }, ...(hasAnyRole('ADMIN') ? [{ to: '/approvals', label: '修改审批', icon: 'A' }] : []), { to: '/analytics', label: '数据看板', icon: 'C' }] : []),
])
const roleName = computed(() => auth.roles.map((role) => ({ ADMIN: '系统管理员', MAINTAINER: '模板管理员', LEADER: '数据领导', REPORTER: '填报人员' }[role] || role)).join('、'))

function formatDeadline(value?: string) {
  return value?.replace('T', ' ').replace(/\.\d+$/, '') || '-'
}

function goToOverdueTask(taskId: number | string) {
  showOverdueReminders.value = false
  router.push({ path: '/late-fill-requests', query: { taskId: String(taskId) } })
}

async function loadOverdueReminders() {
  if (!auth.hasRole('REPORTER')) return
  try {
    overdueReminders.value = (await listTaskReminders()).filter((item) => item.level === 'OVERDUE')
    showOverdueReminders.value = overdueReminders.value.length > 0
  } catch {
    // 提醒查询不应影响已经完成的登录和页面跳转。
  }
}

async function logout() {
  auth.signOut()
  await router.replace({ name: 'login' })
}

onMounted(loadOverdueReminders)
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo"><span>DF</span><strong>数据填报</strong></div>
      <nav><RouterLink v-for="menu in menus" :key="menu.to" :to="menu.to"><b>{{ menu.icon }}</b>{{ menu.label }}</RouterLink></nav>
      <div class="sidebar-note">数据填报管理平台</div>
    </aside>
    <section class="main-shell">
      <header class="topbar">
        <div><strong>{{ auth.user?.realName || auth.user?.username }}</strong><small>{{ roleName }}</small></div>
        <button class="link-button" @click="logout">退出登录</button>
      </header>
      <main class="content"><router-view /></main>
    </section>
    <div v-if="showOverdueReminders" class="modal-backdrop" @click.self="showOverdueReminders = false">
      <section class="detail-modal overdue-reminder-modal" role="dialog" aria-modal="true" aria-labelledby="overdue-reminder-title">
        <header class="detail-modal-header">
          <div>
          <h2 id="overdue-reminder-title">填报任务已逾期</h2>
            <p>以下任务已超过截止时间，可申请指定领导批准补报。</p>
          </div>
          <button class="text-button" @click="showOverdueReminders = false">关闭</button>
        </header>
        <div class="detail-table-scroll">
          <table>
            <thead><tr><th>任务</th><th>模板</th><th>周期</th><th>截止时间</th><th>操作</th></tr></thead>
            <tbody><tr v-for="item in overdueReminders" :key="item.taskId"><td>{{ item.taskName }}</td><td>{{ item.templateName }}</td><td>{{ item.periodLabel || '-' }}</td><td>{{ formatDeadline(item.deadline) }}</td><td><button class="text-button" @click="goToOverdueTask(item.taskId)">申请补报</button></td></tr></tbody>
          </table>
        </div>
        <footer class="detail-pagination"><button class="secondary" @click="showOverdueReminders = false">稍后处理</button></footer>
      </section>
    </div>
  </div>
</template>
