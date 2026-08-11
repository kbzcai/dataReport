<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  NAlert, NButton, NCard, NCollapse, NCollapseItem, NDataTable, NEmpty, NForm, NFormItem,
  NIcon, NInput, NInputNumber, NSelect, NSpace, NSteps, NStep, NTabPane, NTabs, NTag, NUpload, NDatePicker,
  type DataTableColumns, type UploadFileInfo,
} from 'naive-ui'
import { AddOutline, CloudUploadOutline, DownloadOutline, TrashOutline } from '@vicons/ionicons5'
import { downloadTemplate, listTemplates } from '../api/templates'
import { confirmImport, createReports, downloadImportErrors, importPreview, importReport, listImportBatches, type ImportBatch, type ImportSheetPreview } from '../api/reports'
import { listTaskReminders, listTasks, type TaskReminder } from '../api/tasks'
import { listLateFillRequests, type LateFillRequest } from '../api/lateFill'
import { useAuthStore } from '../stores/auth'
import type { ReportTask, Template, TemplateColumn } from '../types'
import '../styles/report-import.css'

type RowData = Record<string, string>
type SheetPreview = ImportSheetPreview & { templateId: string }

const route = useRoute()
const auth = useAuthStore()
const templates = ref<Template[]>([])
const tasks = ref<ReportTask[]>([])
const reminders = ref<TaskReminder[]>([])
const lateFillRequests = ref<LateFillRequest[]>([])
const importBatches = ref<ImportBatch[]>([])
const selectedTaskId = ref('')
const independentTemplateId = ref('')
const activeTab = ref<'online' | 'excel'>('online')
const manualRows = ref<RowData[]>([])
const rowErrors = ref<Record<number, Record<string, string>>>({})
const uploadFiles = ref<UploadFileInfo[]>([])
const selectedFile = ref<File | null>(null)
const autoMatch = ref(false)
const previewSheets = ref<SheetPreview[]>([])
const previewing = ref(false)
const uploading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')

const reporterOnly = computed(() => auth.hasRole('REPORTER') && !auth.hasRole('ADMIN') && !auth.hasRole('LEADER'))
const publishedTasks = computed(() => tasks.value.filter((task) => task.status?.toUpperCase() === 'PUBLISHED'))
const selectedTask = computed(() => publishedTasks.value.find((task) => String(task.id) === selectedTaskId.value))
const selectedTemplate = computed(() => {
  const id = selectedTask.value?.templateId ?? (reporterOnly.value ? undefined : independentTemplateId.value)
  return templates.value.find((item) => String(item.id) === String(id))
})
const templateOptions = computed(() => templates.value.map((template) => ({ label: template.name, value: String(template.id) })))
const selectedReminder = computed(() => reminders.value.find((item) => String(item.taskId) === selectedTaskId.value))
const taskIsOverdue = computed(() => selectedReminder.value?.level === 'OVERDUE' || isOverdue(selectedTask.value?.deadline))
const activeLateFillRequest = computed(() => lateFillRequests.value
  .filter((item) => String(item.taskId) === selectedTaskId.value && item.status === 'APPROVED' && item.lateDeadline)
  .sort((left, right) => new Date(right.lateDeadline!).getTime() - new Date(left.lateDeadline!).getTime())[0])
const hasActiveLateFill = computed(() => Boolean(activeLateFillRequest.value && new Date(activeLateFillRequest.value.lateDeadline!).getTime() > Date.now()))
const taskSubmissionBlocked = computed(() => Boolean(selectedTask.value && taskIsOverdue.value && (reporterOnly.value ? !hasActiveLateFill.value : !selectedTask.value.allowLate)))
const taskStatus = computed(() => {
  if (!selectedTask.value) return reporterOnly.value ? '请选择已发布任务' : '自主填报'
  if (taskIsOverdue.value) return reporterOnly.value ? (hasActiveLateFill.value ? '补报已批准' : '已逾期，待申请') : !selectedTask.value.allowLate ? '已逾期，已关闭' : '已逾期，可补报'
  if (selectedReminder.value?.level === 'DUE_SOON') return '即将截止'
  return '待填报'
})
const importStep = computed(() => autoMatch.value ? (previewSheets.value.length ? 3 : selectedFile.value ? 2 : 1) : selectedFile.value ? 2 : 1)

function msg(caught: unknown) { return caught instanceof Error ? caught.message : '操作失败，请稍后重试' }
function date(value?: string) { return value ? value.replace('T', ' ').replace(/\.\d+$/, '') : '未设置' }
function isOverdue(value?: string) { return Boolean(value && new Date(value).getTime() < Date.now()) }
function deadlineTone(task: ReportTask) { return isOverdue(task.deadline) ? 'error' : reminders.value.find((item) => String(item.taskId) === String(task.id))?.level === 'DUE_SOON' ? 'warning' : 'success' }
function taskLabel(task: ReportTask) { return `${task.name}${task.periodLabel ? `（${task.periodLabel}）` : ''}` }
function detailProgressLabel(task?: ReportTask) {
  const progress = task?.detailProgress
  if (!progress || !progress.totalRows) return '尚未填报明细'
  const parts = [`已填 ${progress.totalRows} 行`]
  if (progress.draftRows) parts.push(`草稿 ${progress.draftRows}`)
  if (progress.submittedRows) parts.push(`已提交 ${progress.submittedRows}`)
  if (progress.returnedRows) parts.push(`已退回 ${progress.returnedRows}`)
  if (progress.approvedRows) parts.push(`已通过 ${progress.approvedRows}`)
  return parts.join(' · ')
}
async function refreshTasks() { tasks.value = await listTasks() }
function newRow(): RowData {
  const row: RowData = {}
  selectedTemplate.value?.columns?.forEach((column) => { row[column.key] = column.defaultValue || '' })
  return row
}
function clearImportState() {
  uploadFiles.value = []
  selectedFile.value = null
  previewSheets.value = []
  previewing.value = false
}
function resetManualRows() {
  manualRows.value = selectedTemplate.value?.columns?.length ? [newRow()] : []
  rowErrors.value = {}
}
function selectTask(taskId: string | number) {
  clearImportState()
  autoMatch.value = false
  selectedTaskId.value = String(taskId)
  activeTab.value = 'online'
  message.value = ''
  error.value = ''
}
function chooseIndependent() {
  if (reporterOnly.value) return
  clearImportState()
  autoMatch.value = false
  selectedTaskId.value = ''
  activeTab.value = 'online'
  message.value = ''
  error.value = ''
}
function addRow() { manualRows.value.push(newRow()) }
function removeRow(index: number) {
  if (manualRows.value.length <= 1) return
  manualRows.value.splice(index, 1)
  rowErrors.value = {}
}
function getOptions(column: TemplateColumn) { return (column.options || []).map((option) => ({ label: option, value: option })) }
function fieldError(column: TemplateColumn, value: unknown) {
  const text = String(value ?? '').trim()
  if (column.required && !text) return '必填'
  if (!text) return ''
  if (column.maxLength && text.length > column.maxLength) return `最多 ${column.maxLength} 个字符`
  if (column.type === 'number' || column.type === 'money') {
    const number = Number(text)
    if (Number.isNaN(number)) return '请输入数字'
    if (column.minValue !== undefined && number < Number(column.minValue)) return `不得小于 ${column.minValue}`
    if (column.maxValue !== undefined && number > Number(column.maxValue)) return `不得大于 ${column.maxValue}`
    if (column.scale !== undefined && !/^[-+]?\d+(\.\d+)?$/.test(text)) return '请输入有效数字'
    if (column.scale !== undefined && (text.split('.')[1]?.length || 0) > column.scale) return `最多 ${column.scale} 位小数`
  }
  if (column.type === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(text)) return '请输入有效日期（YYYY-MM-DD）'
  if (column.type === 'date') { const parsed = new Date(`${text}T00:00:00`); if (Number.isNaN(parsed.getTime()) || `${parsed.getFullYear()}-${String(parsed.getMonth() + 1).padStart(2, '0')}-${String(parsed.getDate()).padStart(2, '0')}` !== text) return '请输入有效日期' }
  if (column.type === 'month' && !/^\d{4}-(0[1-9]|1[0-2])$/.test(text)) return '请输入有效月份（YYYY-MM）'
  if (column.type === 'year' && !/^\d{4}$/.test(text)) return '请输入有效年份（YYYY）'
  if (column.type === 'multiselect') {
    const values = text.split(',').map((item) => item.trim()).filter(Boolean)
    if (!values.length && column.required) return '必填'
    if (column.options?.length && values.some((item) => !column.options!.includes(item))) return '请选择有效选项'
  } else if (column.options?.length && !column.options.includes(text)) return '请选择有效选项'
  if (column.pattern) {
    try { if (!new RegExp(column.pattern).test(text)) return '格式不符合要求' } catch { return '模板正则配置无效' }
  }
  return ''
}
function validateRow(row: RowData, index: number) {
  const errors: Record<string, string> = {}
  selectedTemplate.value?.columns?.forEach((column) => {
    const result = fieldError(column, row[column.key])
    if (result) errors[column.key] = result
  })
  if (Object.keys(errors).length) rowErrors.value = { ...rowErrors.value, [index]: errors }
  else {
    const next = { ...rowErrors.value }
    delete next[index]
    rowErrors.value = next
  }
  return !Object.keys(errors).length
}
function updateValue(row: RowData, rowIndex: number, column: TemplateColumn, value: unknown) {
  row[column.key] = value === null || value === undefined ? '' : Array.isArray(value) ? value.join(',') : String(value)
  validateRow(row, rowIndex)
}
function datePickerValue(value: string, type: 'date' | 'month' | 'year') {
  if (!value) return null
  const parsed = type === 'year' ? new Date(`${value}-01-01T00:00:00`) : type === 'month' ? new Date(`${value}-01T00:00:00`) : new Date(`${value}T00:00:00`)
  return Number.isNaN(parsed.getTime()) ? null : parsed.getTime()
}
function dateType(value?: string): 'date' | 'month' | 'year' {
  return value === 'month' || value === 'year' ? value : 'date'
}
function datePickerText(value: number | null, type: 'date' | 'month' | 'year') {
  if (!value) return ''
  const dateValue = new Date(value)
  const year = dateValue.getFullYear()
  if (type === 'year') return String(year)
  const month = String(dateValue.getMonth() + 1).padStart(2, '0')
  if (type === 'month') return `${year}-${month}`
  return `${year}-${month}-${String(dateValue.getDate()).padStart(2, '0')}`
}
function renderEditor(row: RowData, rowIndex: number, column: TemplateColumn) {
  const errorText = rowErrors.value[rowIndex]?.[column.key]
  const component = column.options?.length
    ? h(NSelect, { value: column.type === 'multiselect' ? row[column.key].split(',').filter(Boolean) : row[column.key], options: getOptions(column), multiple: column.type === 'multiselect', clearable: !column.required, onUpdateValue: (value) => updateValue(row, rowIndex, column, value) })
    : column.type === 'number' || column.type === 'money'
      ? h(NInputNumber, { value: row[column.key] === '' ? null : Number(row[column.key]), min: column.minValue === undefined ? undefined : Number(column.minValue), max: column.maxValue === undefined ? undefined : Number(column.maxValue), showButton: false, onUpdateValue: (value) => updateValue(row, rowIndex, column, value) })
      : ['date', 'month', 'year'].includes(column.type || '')
        ? h(NDatePicker, { value: datePickerValue(row[column.key], dateType(column.type)), type: dateType(column.type), clearable: !column.required, onUpdateValue: (value: number | null) => updateValue(row, rowIndex, column, datePickerText(value, dateType(column.type))) })
      : h(NInput, { value: row[column.key], type: column.type === 'textarea' ? 'textarea' : 'text', maxlength: column.maxLength, placeholder: column.required ? '请输入必填内容' : '可选', onUpdateValue: (value) => updateValue(row, rowIndex, column, value) })
  return h('div', { class: 'report-cell-editor' }, [component, errorText ? h('small', { class: 'field-error' }, errorText) : null])
}
const manualColumns = computed<DataTableColumns<RowData>>(() => [
  { title: '#', key: '__index', width: 58, fixed: 'left', render: (_, index) => String(index + 1) },
  ...(selectedTemplate.value?.columns || []).map((column) => ({
    title: () => h('span', { class: 'report-column-title' }, [column.label, column.required ? h('em', {}, ' *') : null]),
    key: column.key,
    minWidth: 180,
    render: (row: RowData, index: number) => renderEditor(row, index, column),
  })),
  { title: '操作', key: '__actions', width: 76, fixed: 'right', render: (_, index) => h(NButton, { text: true, type: 'error', disabled: manualRows.value.length === 1, onClick: () => removeRow(index) }, { icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) }) },
])
const batchColumns: DataTableColumns<ImportBatch> = [
  { title: '文件', key: 'originalFileName', minWidth: 180 },
  { title: '时间', key: 'createdAt', width: 165, render: (row) => date(row.createdAt) },
  { title: '结果', key: 'result', width: 120, render: (row) => `${row.importedRows} 成功 / ${row.failedRows} 失败` },
  { title: '状态', key: 'status', width: 100, render: (row) => h(NTag, { type: row.failedRows ? 'warning' : 'success', size: 'small', bordered: false }, { default: () => row.status }) },
  { title: '摘要', key: 'summary', minWidth: 180, ellipsis: { tooltip: true } },
  { title: '操作', key: 'action', width: 105, render: (row) => row.failedRows > 0 ? h(NButton, { text: true, type: 'primary', onClick: () => downloadImportErrors(row.id) }, { default: () => '下载错误' }) : '-' },
]
async function download() {
  if (!selectedTemplate.value) return
  try { await downloadTemplate(selectedTemplate.value.id, selectedTemplate.value.name) } catch (caught) { error.value = msg(caught) }
}
function updateUpload(files: UploadFileInfo[]) {
  uploadFiles.value = files.slice(-1)
  selectedFile.value = uploadFiles.value[0]?.file || null
  previewSheets.value = []
  message.value = ''
  error.value = ''
  if (autoMatch.value && selectedFile.value) previewWorkbook()
}
async function previewWorkbook() {
  if (!selectedFile.value) { error.value = '请选择 Excel 文件'; return }
  previewing.value = true
  error.value = ''
  try {
    const result = await importPreview(selectedFile.value)
    previewSheets.value = result.sheets.map((sheet) => ({ ...sheet, templateId: sheet.suggestedTemplateId ? String(sheet.suggestedTemplateId) : '' }))
  } catch (caught) { error.value = msg(caught) } finally { previewing.value = false }
}
async function submitImport() {
  if (!selectedFile.value) { error.value = '请选择填写完成的 Excel 文件'; return }
  if (reporterOnly.value && !selectedTask.value) { error.value = '请选择已发布的填报任务'; return }
  if (!autoMatch.value && !selectedTemplate.value) { error.value = '请选择填报任务或自主填报模板'; return }
  if (taskSubmissionBlocked.value) { error.value = reporterOnly.value ? '该任务已逾期，无法提交' : '该任务已逾期且不允许补报，无法提交'; return }
  if (autoMatch.value && !previewSheets.value.length) { await previewWorkbook(); return }
  if (autoMatch.value && previewSheets.value.some((sheet) => !sheet.templateId)) { error.value = '请为每个工作表选择导入模板'; return }
  uploading.value = true
  error.value = ''
  try {
    const result = autoMatch.value
      ? await confirmImport(selectedFile.value, previewSheets.value.map((sheet) => sheet.templateId))
      : await importReport(selectedTemplate.value!.id, selectedFile.value, selectedTask.value?.id)
    message.value = `导入批次 ${result.batchId} 已完成：成功 ${result.importedRows} 行，失败 ${result.failedRows} 行`
    clearImportState()
    try {
      const [loadedBatches] = await Promise.all([listImportBatches(), refreshTasks()])
      importBatches.value = loadedBatches
    } catch {
      // 导入已经成功，列表刷新失败不应覆盖成功结果。
    }
  } catch (caught) { error.value = msg(caught) } finally { uploading.value = false }
}
async function submitManual() {
  if (reporterOnly.value && !selectedTask.value) { error.value = '请选择已发布的填报任务'; return }
  if (!selectedTemplate.value) { error.value = '请选择填报任务或自主填报模板'; return }
  if (taskSubmissionBlocked.value) { error.value = reporterOnly.value ? '该任务已逾期，无法提交' : '该任务已逾期且不允许补报，无法提交'; return }
  const valid = manualRows.value.map((row, index) => validateRow(row, index)).every(Boolean)
  if (!valid) { error.value = '请修正标红的字段后再提交'; return }
  saving.value = true
  error.value = ''
  try {
    await createReports(manualRows.value.map((data) => ({ templateId: selectedTemplate.value!.id, taskId: selectedTask.value?.id, data, status: 'SUBMITTED' })))
    message.value = `已提交 ${manualRows.value.length} 行填报数据`
    resetManualRows()
    try { await refreshTasks() } catch {
      // 提交已成功，稍后刷新页面即可重新获取最新明细进度。
    }
  } catch (caught) { error.value = msg(caught) } finally { saving.value = false }
}
async function load() {
  try {
    const [loadedTemplates, loadedTasks, loadedBatches] = await Promise.all([listTemplates(), listTasks(), listImportBatches()])
    templates.value = loadedTemplates
    tasks.value = loadedTasks
    importBatches.value = loadedBatches
    const queryTaskId = route.query.taskId
    if (typeof queryTaskId === 'string' && publishedTasks.value.some((task) => String(task.id) === queryTaskId)) selectedTaskId.value = queryTaskId
  } catch (caught) { error.value = msg(caught) }
  try { reminders.value = await listTaskReminders() } catch { reminders.value = [] }
  if (reporterOnly.value) {
    try { lateFillRequests.value = await listLateFillRequests() } catch { lateFillRequests.value = [] }
  }
}
watch(selectedTemplate, resetManualRows)
watch(() => route.query.taskId, (taskId) => {
  if (typeof taskId === 'string' && publishedTasks.value.some((task) => String(task.id) === taskId)) selectTask(taskId)
})
watch(autoMatch, (enabled) => {
  if (enabled && reporterOnly.value) {
    autoMatch.value = false
    return
  }
  if (enabled) {
    // 自动匹配是无任务导入，避免沿用已选任务的截止时间和任务编号语义。
    selectedTaskId.value = ''
    independentTemplateId.value = ''
    if (selectedFile.value && !previewSheets.value.length) previewWorkbook()
  } else {
    previewSheets.value = []
  }
})
onMounted(load)
</script>

<template>
  <section class="report-workbench">
    <div class="page-heading report-heading">
      <div><h1>数据填报</h1><p>先选择待填任务，再使用在线填报或 Excel 导入完成提交。</p></div>
    </div>

    <n-alert v-if="message" type="success" closable class="workbench-notice" @close="message = ''">{{ message }}</n-alert>
    <n-alert v-if="error" type="error" closable class="workbench-notice" @close="error = ''">{{ error }}</n-alert>
    <n-alert v-if="selectedTask && taskIsOverdue" :type="taskSubmissionBlocked ? 'error' : 'warning'" class="workbench-notice" :show-icon="true">
      <span>{{ taskSubmissionBlocked ? (reporterOnly ? '当前任务已逾期，请先提交补报申请并等待指定领导批准。' : '当前任务已逾期，在线填报和 Excel 导入已禁用。') : '补报已批准，请在批准截止时间前完成提交。' }}</span>
      <n-button v-if="reporterOnly && taskSubmissionBlocked" text type="primary" @click="$router.push({ path: '/late-fill-requests', query: { taskId: selectedTaskId } })">申请补报</n-button>
      <span v-if="reporterOnly && hasActiveLateFill">批准截止：{{ date(activeLateFillRequest?.lateDeadline) }}</span>
    </n-alert>
    <n-alert v-else-if="selectedTask && selectedReminder?.level === 'DUE_SOON'" type="warning" class="workbench-notice" :show-icon="true">该任务即将截止，请及时完成填报。</n-alert>

    <n-card class="task-center" title="待填任务" :bordered="false">
      <template #header-extra><n-tag size="small" type="info" :bordered="false">{{ publishedTasks.length }} 个任务</n-tag></template>
      <div v-if="publishedTasks.length" class="task-list">
        <button v-for="task in publishedTasks" :key="task.id" type="button" class="task-card" :class="{ selected: selectedTaskId === String(task.id) }" @click="selectTask(task.id)">
          <span class="task-card-title">{{ task.name }}</span>
          <span class="task-card-template">{{ task.templateName || '关联模板' }}{{ task.periodLabel ? ` · ${task.periodLabel}` : '' }}</span>
          <span class="task-card-meta">截止：{{ date(task.deadline) }}</span>
          <span class="task-card-progress">{{ detailProgressLabel(task) }}</span>
          <n-tag size="small" :type="deadlineTone(task)" :bordered="false">{{ isOverdue(task.deadline) ? (reporterOnly ? (String(task.id) === selectedTaskId && hasActiveLateFill ? '补报已批准' : '已逾期') : !task.allowLate ? '已逾期' : '已逾期，可补报') : reminders.find((item) => String(item.taskId) === String(task.id))?.level === 'DUE_SOON' ? '即将截止' : '待填报' }}</n-tag>
        </button>
      </div>
      <n-empty v-else description="当前没有已发布的填报任务" size="small" />
      <div v-if="!reporterOnly" class="independent-entry">
        <n-button tertiary type="primary" @click="chooseIndependent">自主填报</n-button>
        <span>无任务时可自行选择启用模板填报，数据不会关联到任务。</span>
      </div>
    </n-card>

    <n-card class="selected-task-card" :bordered="false">
      <div class="selected-task-summary">
        <div>
          <span class="eyebrow">{{ selectedTask ? '当前任务' : reporterOnly ? '请选择任务' : '自主填报' }}</span>
          <h2>{{ selectedTask?.name || (reporterOnly ? '请选择已发布任务' : '选择填报模板') }}</h2>
          <p v-if="selectedTask">{{ selectedTask.description || '请按任务要求完成本期数据填报。' }}</p>
          <p v-else>{{ reporterOnly ? '请选择一个已发布任务后开始填报。' : '请选择一个启用模板后开始填报。' }}</p>
        </div>
        <div class="task-facts">
          <span>模板：<strong>{{ selectedTemplate?.name || selectedTask?.templateName || '未选择' }}</strong></span>
          <span>截止：<strong>{{ selectedTask ? date(selectedTask.deadline) : '-' }}</strong></span>
          <span v-if="selectedTask">明细：<strong>{{ detailProgressLabel(selectedTask) }}</strong></span>
          <n-tag :type="taskIsOverdue ? 'error' : selectedReminder?.level === 'DUE_SOON' ? 'warning' : 'success'" :bordered="false">{{ taskStatus }}</n-tag>
        </div>
      </div>
      <n-form v-if="!selectedTask && !reporterOnly" label-placement="top" class="independent-template-form">
        <n-form-item label="填报模板">
          <n-select v-model:value="independentTemplateId" :options="templateOptions" placeholder="请选择启用模板" filterable clearable />
        </n-form-item>
      </n-form>
    </n-card>

    <n-tabs v-model:value="activeTab" type="line" animated class="report-tabs" display-directive="show">
      <n-tab-pane name="online" tab="在线填报">
        <n-card :bordered="false" class="entry-card">
          <template #header><span>在线填报</span></template>
          <template #header-extra><n-tag v-if="selectedTemplate" size="small" :bordered="false">{{ selectedTemplate.columns?.length || 0 }} 个字段</n-tag></template>
          <n-empty v-if="!selectedTemplate" :description="reporterOnly ? '请先选择一个已发布任务' : '请先选择一个待填任务或自主填报模板'" />
          <n-empty v-else-if="!selectedTemplate.columns?.length" description="该模板暂无字段，请联系模板管理员维护表头" />
          <template v-else>
            <n-data-table class="desktop-report-table" :columns="manualColumns" :data="manualRows" :bordered="false" :single-line="false" :scroll-x="Math.max(760, (selectedTemplate.columns.length + 2) * 190)" />
            <div class="mobile-report-form">
              <n-form v-for="(row, rowIndex) in manualRows" :key="rowIndex" label-placement="top" class="mobile-row-card">
                <div class="mobile-row-header"><strong>第 {{ rowIndex + 1 }} 行</strong><n-button text type="error" :disabled="manualRows.length === 1" @click="removeRow(rowIndex)">删除</n-button></div>
                <n-form-item v-for="column in selectedTemplate.columns" :key="column.key" :label="column.label" :validation-status="rowErrors[rowIndex]?.[column.key] ? 'error' : undefined" :feedback="rowErrors[rowIndex]?.[column.key]">
                  <n-select v-if="column.options?.length" :value="column.type === 'multiselect' ? row[column.key].split(',').filter(Boolean) : row[column.key]" :options="getOptions(column)" :multiple="column.type === 'multiselect'" :clearable="!column.required" @update:value="(value) => updateValue(row, rowIndex, column, value)" />
                  <n-input-number v-else-if="column.type === 'number' || column.type === 'money'" :value="row[column.key] === '' ? null : Number(row[column.key])" :min="column.minValue === undefined ? undefined : Number(column.minValue)" :max="column.maxValue === undefined ? undefined : Number(column.maxValue)" :show-button="false" @update:value="(value) => updateValue(row, rowIndex, column, value)" />
                  <n-date-picker v-else-if="column.type === 'date' || column.type === 'month' || column.type === 'year'" :value="datePickerValue(row[column.key], dateType(column.type))" :type="dateType(column.type)" :clearable="!column.required" @update:value="(value) => updateValue(row, rowIndex, column, datePickerText(value, dateType(column.type)))" />
                  <n-input v-else :value="row[column.key]" :type="column.type === 'textarea' ? 'textarea' : 'text'" :maxlength="column.maxLength" :placeholder="column.required ? '请输入必填内容' : '可选'" @update:value="(value) => updateValue(row, rowIndex, column, value)" />
                </n-form-item>
              </n-form>
            </div>
            <div class="sticky-submit-bar">
              <n-space align="center" justify="space-between" :wrap="true">
                <n-button :disabled="taskSubmissionBlocked" @click="addRow"><template #icon><n-icon><AddOutline /></n-icon></template>新增一行</n-button>
                <span>已填 {{ manualRows.length }} 行</span>
                <n-button type="primary" :loading="saving" :disabled="taskSubmissionBlocked" @click="submitManual">提交填报</n-button>
              </n-space>
            </div>
          </template>
        </n-card>
      </n-tab-pane>

      <n-tab-pane name="excel" tab="Excel 导入">
        <n-card :bordered="false" class="entry-card">
          <template #header>Excel 导入</template>
          <template #header-extra><n-button v-if="selectedTemplate" text type="primary" @click="download"><template #icon><n-icon><DownloadOutline /></n-icon></template>下载模板</n-button></template>
          <n-steps :current="importStep" size="small" class="import-steps">
            <n-step title="上传文件" />
            <n-step :title="autoMatch ? '解析预览' : '已选择/校验'" />
            <n-step title="确认导入" />
          </n-steps>
          <n-alert v-if="!selectedTemplate && !autoMatch" type="info" class="import-alert">{{ reporterOnly ? '请先选择已发布任务，再上传对应 Excel 文件。' : '请先选择待填任务或自主填报模板，再上传对应 Excel 文件。' }}</n-alert>
          <n-upload accept=".xlsx,.xls" :default-upload="false" :file-list="uploadFiles" :max="1" @update:file-list="updateUpload">
            <n-button><template #icon><n-icon><CloudUploadOutline /></n-icon></template>选择 Excel 文件</n-button>
          </n-upload>
          <p v-if="selectedFile" class="selected-file">已选择：{{ selectedFile.name }}</p>
          <n-alert v-if="selectedFile && !autoMatch" type="success" :show-icon="false" class="file-check-alert">文件已选择，前端已完成格式和任务状态校验，可确认导入。</n-alert>
          <n-collapse v-if="!reporterOnly" class="advanced-import">
            <n-collapse-item title="更多导入选项" name="advanced">
              <n-alert type="warning" :show-icon="false">多工作表自动匹配不会关联当前任务，即使当前已选择任务，导入记录也不会带入任务编号。</n-alert>
              <n-button :type="autoMatch ? 'primary' : 'default'" size="small" class="auto-match-button" @click="autoMatch = !autoMatch">{{ autoMatch ? '已启用多工作表自动匹配' : '启用多工作表自动匹配' }}</n-button>
            </n-collapse-item>
          </n-collapse>
          <div v-if="autoMatch && previewSheets.length" class="sheet-preview-workbench">
            <n-data-table :columns="[
              { title: '顺序', key: 'sheetOrder', width: 72, render: (row: SheetPreview) => row.sheetOrder + 1 },
              { title: '工作表', key: 'sheetName', minWidth: 130 },
              { title: '识别结果', key: 'matchStatus', minWidth: 170, render: (row: SheetPreview) => `${row.matchStatus === 'NAME' ? '名称匹配' : row.matchStatus === 'HEADER' ? '表头匹配' : row.matchStatus === 'AMBIGUOUS' ? '匹配不唯一' : '未匹配'}${row.suggestedTemplateName ? ` · 建议：${row.suggestedTemplateName}` : ''}` },
              { title: '导入模板', key: 'templateId', minWidth: 200, render: (row: SheetPreview) => h(NSelect, { value: row.templateId, options: templateOptions, placeholder: '请选择模板', onUpdateValue: (value) => { row.templateId = String(value) } }) },
            ]" :data="previewSheets" :bordered="false" />
          </div>
          <div class="import-actions">
            <n-button v-if="autoMatch" :loading="previewing" :disabled="!selectedFile" @click="previewWorkbook">解析预览</n-button>
            <n-button type="primary" :loading="uploading" :disabled="taskSubmissionBlocked || !selectedFile" @click="submitImport">确认导入</n-button>
          </div>
        </n-card>
      </n-tab-pane>
    </n-tabs>

    <n-card class="batch-card" title="导入批次" :bordered="false">
      <template #header-extra><span class="batch-hint">失败批次可下载错误清单</span></template>
      <n-data-table :columns="batchColumns" :data="importBatches" :bordered="false" :single-line="false" :pagination="false" />
    </n-card>
  </section>
</template>
