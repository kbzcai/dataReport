<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listTaskReminders, type TaskReminder } from '../api/tasks'
const reminders = ref<TaskReminder[]>([]); const error = ref(''); const loading = ref(false)
function date(value?: string) { return value?.replace('T', ' ').replace(/\.\d+$/, '') || '-' }
async function load() { loading.value = true; try { reminders.value = await listTaskReminders() } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' } finally { loading.value = false } }
onMounted(load)
</script>

<template><section><div class="page-heading"><div><h1>填报提醒</h1><p>显示当前待填报任务及其截止状态。</p></div><button class="secondary" @click="load">刷新</button></div><div v-if="error" class="notice error">{{ error }}</div><div class="table-wrap"><table><thead><tr><th>任务</th><th>模板</th><th>周期</th><th>截止时间</th><th>提醒状态</th></tr></thead><tbody><tr v-if="loading"><td colspan="5" class="muted">加载中...</td></tr><tr v-else-if="!reminders.length"><td colspan="5" class="muted">当前没有待填报任务。</td></tr><tr v-for="item in reminders" :key="item.taskId"><td>{{ item.taskName }}</td><td>{{ item.templateName }}</td><td>{{ item.periodLabel || '-' }}</td><td>{{ date(item.deadline) }}</td><td><span class="status">{{ item.level === 'OVERDUE' ? '已逾期' : item.level === 'DUE_SOON' ? '即将截止' : '待填报' }}</span></td></tr></tbody></table></div></section></template>
