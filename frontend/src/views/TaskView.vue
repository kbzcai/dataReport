<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listTemplateVersions, listTemplates } from '../api/templates'
import { createTask, deleteTask, listAssignableTargets, listTasks, updateTask, type AssignableTarget } from '../api/tasks'
import { exportReports } from '../api/reports'
import type { ReportTask, Template, TemplateVersion } from '../types'
import TaskTargetTreeNode from './TaskTargetTreeNode.vue'

const tasks = ref<ReportTask[]>([])
const templates = ref<Template[]>([])
const versions = ref<TemplateVersion[]>([])
const targets = ref<AssignableTarget[]>([])
const editing = ref<ReportTask | null>(null)
const showEditor = ref(false)
const loading = ref(false)
const message = ref('')
const error = ref('')
const exportingTaskId = ref<number | string | null>(null)
const form = ref({ name: '', templateId: '', templateVersionId: '', frequency: 'MONTHLY', periodLabel: '', startAt: '', deadline: '', allowLate: false, status: 'DRAFT', description: '', assigneeIds: [] as Array<number | string>, departmentIds: [] as Array<number | string> })
const frequencyNames: Record<string, string> = { DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月', QUARTERLY: '每季度', YEARLY: '每年', CUSTOM: '自定义' }
const statusNames: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }
const targetsByParent = computed(() => {
  const ids = new Set(targets.value.map((target) => String(target.id)))
  const result = new Map<string, AssignableTarget[]>()
  for (const target of targets.value) {
    const key = target.parentId != null && ids.has(String(target.parentId)) ? String(target.parentId) : 'root'
    result.set(key, [...(result.get(key) || []), target].sort((left, right) => left.name.localeCompare(right.name)))
  }
  return result
})
const rootTargets = computed(() => targetsByParent.value.get('root') || [])

function msg(error: unknown) { return error instanceof Error ? error.message : '操作失败' }
function selected(ids: Array<number | string>, id: number | string) { return ids.some((value) => String(value) === String(id)) }
function toggleDepartment(id: number | string, checked: boolean) {
  form.value.departmentIds = checked ? [...new Set([...form.value.departmentIds.map(String), String(id)])] : form.value.departmentIds.filter((value) => String(value) !== String(id))
}
function toggleAssignee(id: number | string, checked: boolean) {
  form.value.assigneeIds = checked ? [...new Set([...form.value.assigneeIds.map(String), String(id)])] : form.value.assigneeIds.filter((value) => String(value) !== String(id))
}
async function load() { loading.value = true; try { tasks.value = await listTasks() } catch (caught) { error.value = msg(caught) } finally { loading.value = false } }
async function loadVersions(templateId: string, preferred?: number | string) {
  try { versions.value = templateId ? await listTemplateVersions(templateId) : []; form.value.templateVersionId = preferred == null ? (versions.value[0] ? String(versions.value[0].id) : '') : String(preferred) } catch (caught) { error.value = msg(caught) }
}
function openCreate() {
  editing.value = null
  const templateId = templates.value[0] ? String(templates.value[0].id) : ''
  form.value = { name: '', templateId, templateVersionId: '', frequency: 'MONTHLY', periodLabel: '', startAt: '', deadline: '', allowLate: false, status: 'DRAFT', description: '', assigneeIds: [], departmentIds: [] }
  showEditor.value = true
  void loadVersions(templateId)
}
function openEdit(task: ReportTask) {
  editing.value = task
  form.value = { name: task.name, templateId: String(task.templateId), templateVersionId: task.templateVersionId == null ? '' : String(task.templateVersionId), frequency: task.frequency, periodLabel: task.periodLabel || '', startAt: task.startAt?.slice(0, 16) || '', deadline: task.deadline?.slice(0, 16) || '', allowLate: Boolean(task.allowLate), status: task.status, description: task.description || '', assigneeIds: [...(task.assigneeIds || [])], departmentIds: [...(task.departmentIds || [])] }
  showEditor.value = true
  void loadVersions(String(task.templateId), task.templateVersionId)
}
function cancel() { showEditor.value = false; editing.value = null }
async function save() {
  if (!form.value.name.trim() || !form.value.templateId) { error.value = '任务名称和模板不能为空'; return }
  if (!form.value.departmentIds.length && !form.value.assigneeIds.length) { error.value = '请至少选择一个部门或填报人员，任务不能以开放范围发布'; return }
  error.value = ''
  const data = { ...form.value, templateId: Number(form.value.templateId), templateVersionId: form.value.templateVersionId ? Number(form.value.templateVersionId) : undefined, startAt: form.value.startAt || undefined, deadline: form.value.deadline || undefined, assigneeIds: form.value.assigneeIds.map(Number), departmentIds: form.value.departmentIds.map(Number) }
  try { if (editing.value) await updateTask(editing.value.id, data); else await createTask(data); message.value = editing.value ? '任务已更新' : '任务已创建'; cancel(); await load() } catch (caught) { error.value = msg(caught) }
}
async function remove(task: ReportTask) {
  if (!window.confirm(`确认删除任务“${task.name}”吗？没有填报数据的任务才允许删除。`)) return
  try { await deleteTask(task.id); message.value = '任务已删除'; await load() } catch (caught) { error.value = msg(caught) }
}
function progress(task: ReportTask) { const item = task.progress; return !item?.assigneeCount ? '无有效填报对象' : `${item.submittedAssigneeCount} 已提交 / ${item.pendingAssigneeCount} 待填报` }
function detailProgress(task: ReportTask) {
  const item = task.detailProgress
  if (!item || !item.totalRows) return '尚无明细'
  return `${item.totalRows} 行（草稿 ${item.draftRows}，已提交 ${item.submittedRows}，退回 ${item.returnedRows}，已通过 ${item.approvedRows}）`
}
async function exportTask(task: ReportTask) {
  if (exportingTaskId.value !== null) return
  exportingTaskId.value = task.id
  error.value = ''
  try { await exportReports(task.templateId, task.id) } catch (caught) { error.value = msg(caught) } finally { exportingTaskId.value = null }
}
function scopeLabel(task: ReportTask) {
  const departments = (task.departmentIds || []).map((id) => targets.value.find((target) => String(target.id) === String(id))?.name || `部门#${id}`)
  const assignees = task.assignees?.map((user) => user.username) || []
  return [...departments, ...assignees].join('、') || '未配置'
}

onMounted(async () => { try { [templates.value, targets.value] = await Promise.all([listTemplates(), listAssignableTargets()]) } catch (caught) { error.value = msg(caught) }; await load() })
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>填报任务</h1><p>发布模板版本，并向本部门及下级部门的领导或填报人员指派任务。</p></div><button class="primary" @click="openCreate">新建任务</button></div>
    <div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div>
    <form v-if="showEditor" class="editor" @submit.prevent="save">
      <h2>{{ editing ? '编辑任务' : '新建任务' }}</h2>
      <div class="form-grid">
        <label>任务名称<input v-model.trim="form.name" maxlength="128" placeholder="例如：2026年8月月度经营填报" /></label>
        <label>关联模板<select v-model="form.templateId" @change="loadVersions(form.templateId)"><option value="">请选择模板</option><option v-for="item in templates" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select></label>
        <label>模板版本<select v-model="form.templateVersionId"><option v-for="version in versions" :key="version.id" :value="String(version.id)">V{{ version.versionNo }}（{{ version.status }}）</option></select></label>
        <label>填报频率<select v-model="form.frequency"><option v-for="(label, key) in frequencyNames" :key="key" :value="key">{{ label }}</option></select></label>
        <label>周期标识<input v-model.trim="form.periodLabel" placeholder="例如：2026-08" /></label><label>开始时间<input v-model="form.startAt" type="datetime-local" /></label>
        <label>截止时间<input v-model="form.deadline" type="datetime-local" /></label><label class="checkbox-label"><input v-model="form.allowLate" type="checkbox" />允许逾期填报</label>
        <label>任务状态<select v-model="form.status"><option value="DRAFT">草稿</option><option value="PUBLISHED">发布</option><option value="CLOSED">关闭</option></select></label><label>任务说明<input v-model.trim="form.description" maxlength="500" /></label>
      </div>
      <fieldset class="target-tree"><legend>任务范围</legend><p class="hint">至少选择一个部门或人员。选择部门会覆盖该部门及其下级部门中可填报的人员。</p><ul v-if="rootTargets.length" class="tree-list"><li v-for="target in rootTargets" :key="target.id"><TaskTargetTreeNode :target="target" :children-by-parent="targetsByParent" :department-ids="form.departmentIds" :assignee-ids="form.assigneeIds" @toggle-department="toggleDepartment" @toggle-assignee="toggleAssignee" /></li></ul><p v-else class="muted">当前部门范围内没有可指派对象。</p></fieldset>
      <div class="actions"><button class="primary">保存</button><button type="button" class="secondary" @click="cancel">取消</button></div>
    </form>
    <div class="table-wrap"><table><thead><tr><th>任务名称</th><th>模板版本</th><th>频率/周期</th><th>指派范围</th><th>人员进度</th><th>明细进度</th><th>截止时间</th><th>逾期</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr v-if="loading"><td colspan="10" class="muted">加载中...</td></tr><tr v-else-if="!tasks.length"><td colspan="10" class="muted">暂无任务。</td></tr>
      <tr v-for="task in tasks" :key="task.id"><td>{{ task.name }}</td><td>{{ task.templateName }} V{{ task.templateVersionNo || '-' }}</td><td>{{ frequencyNames[task.frequency] || task.frequency }}{{ task.periodLabel ? ` / ${task.periodLabel}` : '' }}</td><td>{{ scopeLabel(task) }}</td><td>{{ progress(task) }}</td><td>{{ detailProgress(task) }}</td><td>{{ task.deadline ? task.deadline.replace('T', ' ') : '-' }}</td><td>{{ task.allowLate ? '允许' : '不允许' }}</td><td>{{ statusNames[task.status] || task.status }}</td><td><button class="text-button" :disabled="exportingTaskId !== null" @click="exportTask(task)">{{ exportingTaskId === task.id ? '导出中...' : '导出 Excel' }}</button><span v-if="task.sourceType === 'SCHEDULED'" class="muted">定时发布，只读</span><template v-else><button class="text-button" @click="openEdit(task)">修改</button><button class="danger-button" @click="remove(task)">删除</button></template></td></tr>
    </tbody></table></div>
  </section>
</template>
