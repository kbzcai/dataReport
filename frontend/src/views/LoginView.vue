<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')
const router = useRouter()
const auth = useAuthStore()

async function submit() {
  if (!username.value || !password.value) { error.value = '请输入用户名和密码'; return }
  submitting.value = true
  error.value = ''
  try {
    await auth.signIn(username.value, password.value)
    const destination = auth.role === 'LEADER' ? '/reports' : '/reports/import'
    await router.replace(destination)
    if (router.currentRoute.value.path !== destination) window.location.replace(destination)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败'
  } finally { submitting.value = false }
}
</script>

<template>
  <main class="login-page">
    <form class="login-panel" @submit.prevent="submit">
      <div class="brand-mark">DF</div>
      <h1>数据填报系统</h1>
      <p>请使用系统账号登录</p>
      <label>用户名<input v-model.trim="username" autocomplete="username" placeholder="请输入用户名" /></label>
      <label>密码<input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码" /></label>
      <div v-if="error" class="error-text">{{ error }}</div>
      <button class="primary wide" :disabled="submitting">{{ submitting ? '登录中...' : '登录' }}</button>
    </form>
  </main>
</template>
