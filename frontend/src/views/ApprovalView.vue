<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { approveChangeRequest, listChangeRequests, rejectChangeRequest, type ChangeRequest } from '../api/changeRequests'
const requests = ref<ChangeRequest[]>([]); const error = ref(''); const message = ref(''); const loading = ref(false)
function text(e: unknown) { return e instanceof Error ? e.message : '操作失败' }
async function load() { loading.value = true; try { requests.value = await listChangeRequests('PENDING') } catch (e) { error.value = text(e) } finally { loading.value = false } }
async function approve(item: ChangeRequest) { try { await approveChangeRequest(item.id); message.value = '修改申请已通过'; await load() } catch (e) { error.value = text(e) } }
async function reject(item: ChangeRequest) { const comment = window.prompt('请输入驳回意见'); if (!comment?.trim()) return; try { await rejectChangeRequest(item.id, comment.trim()); message.value = '修改申请已驳回'; await load() } catch (e) { error.value = text(e) } }
function preview(item: ChangeRequest) { return Object.values(item.proposedData || {}).map(value => String(value)).join(' | ') }
onMounted(load)
</script>

<template><section><div class="page-heading"><div><h1>修改审批</h1><p>审批填报人员提交的记录修改申请。</p></div><button class="secondary" @click="load">刷新</button></div><div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div><div class="table-wrap"><table><thead><tr><th>申请人</th><th>任务/模板</th><th>修改原因</th><th>申请数据</th><th>申请时间</th><th class="fixed-action">操作</th></tr></thead><tbody><tr v-if="loading"><td colspan="6" class="muted">加载中...</td></tr><tr v-else-if="!requests.length"><td colspan="6" class="muted">暂无待审批申请。</td></tr><tr v-for="item in requests" :key="item.id"><td>{{ item.requesterName }}</td><td>{{ item.taskName || '-' }} / {{ item.templateName }}</td><td>{{ item.reason }}</td><td class="data-cell">{{ preview(item) }}</td><td>{{ item.createdAt?.replace('T',' ') }}</td><td class="fixed-action"><button class="text-button" @click="approve(item)">通过</button><button class="danger-button" @click="reject(item)">驳回</button></td></tr></tbody></table></div></section></template>
