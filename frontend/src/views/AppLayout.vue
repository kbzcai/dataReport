<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const menus = computed(() => [
  ...(auth.role === 'ADMIN' ? [{ to: '/users', label: '权限管理', icon: 'U' }] : []),
  ...(['ADMIN', 'LEADER'].includes(auth.role) ? [{ to: '/tasks', label: '填报任务', icon: 'K' }] : []),
  ...(['MAINTAINER', 'ADMIN'].includes(auth.role) ? [{ to: '/templates', label: '模板维护', icon: 'T' }] : []),
  ...(['REPORTER', 'ADMIN', 'LEADER'].includes(auth.role) ? [{ to: '/reports/import', label: '数据填报', icon: 'F' }] : []),
  ...(auth.role === 'REPORTER' ? [{ to: '/my-reports', label: '我的填报', icon: 'M' }] : []),
  ...(auth.role === 'REPORTER' ? [{ to: '/reminders', label: '填报提醒', icon: 'R' }] : []),
  ...(['LEADER', 'ADMIN'].includes(auth.role) ? [{ to: '/reports', label: '填报数据', icon: 'D' }] : []),
  ...(['LEADER', 'ADMIN'].includes(auth.role) ? [{ to: '/approvals', label: '修改审批', icon: 'A' }] : []),
  ...(['LEADER', 'ADMIN'].includes(auth.role) ? [{ to: '/analytics', label: '数据看板', icon: 'C' }] : []),
])
const roleName = computed(() => ({ ADMIN: '系统管理员', MAINTAINER: '模板管理员', LEADER: '数据领导', REPORTER: '填报人员' }[auth.role] || auth.role))
async function logout() {
  auth.signOut()
  await router.replace('/login')
  if (router.currentRoute.value.path !== '/login') window.location.replace('/login')
}
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
  </div>
</template>
