<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { downloadTemplate, listTemplates } from '../api/templates'
import { confirmImport, createReports, downloadImportErrors, importPreview, importReport, listImportBatches, type ImportBatch, type ImportSheetPreview } from '../api/reports'
import { listTasks } from '../api/tasks'
import type { ReportTask, Template } from '../types'

const templates = ref<Template[]>([])
const tasks = ref<ReportTask[]>([])
const importMode = ref<'manual' | 'auto'>('manual')
const templateId = ref('')
const importTaskId = ref('')
const manualTaskId = ref('')
const file = ref<File | null>(null)
const previewSheets = ref<Array<ImportSheetPreview & { templateId: string }>>([])
const importBatches = ref<ImportBatch[]>([])
const previewing = ref(false)
const manualRows = ref<Record<string, string>[]>([])
const uploading = ref(false)
const saving = ref(false)
const message = ref('')
const error = ref('')
const selectedTemplate = computed(() => templates.value.find((item) => String(item.id) === templateId.value))
const publishedTasks = computed(() => tasks.value.filter((task) => task.status?.toUpperCase() === 'PUBLISHED'))
const selectedManualTask = computed(() => publishedTasks.value.find((task) => String(task.id) === manualTaskId.value))
const selectedImportTask = computed(() => publishedTasks.value.find((task) => String(task.id) === importTaskId.value))
async function load() {
  try {
    const [loadedTemplates, loadedTasks, loadedBatches] = await Promise.all([listTemplates(), listTasks(), listImportBatches()])
    templates.value = loadedTemplates
    tasks.value = loadedTasks
    importBatches.value = loadedBatches
    if (!templateId.value && templates.value.length) templateId.value = String(templates.value[0].id)
  } catch (e) { error.value = msg(e) }
}
function newRow() { const row: Record<string, string> = {}; selectedTemplate.value?.columns?.forEach((column) => { row[column.key] = '' }); return row }
function resetManualRows() { manualRows.value = selectedTemplate.value?.columns?.length ? [newRow()] : [] }
watch(selectedTemplate, resetManualRows)
watch(manualTaskId, (taskId) => {
  const task = publishedTasks.value.find((item) => String(item.id) === taskId)
  if (task) {
    importTaskId.value = ''
    templateId.value = String(task.templateId)
  }
})
watch(importTaskId, (taskId) => {
  const task = publishedTasks.value.find((item) => String(item.id) === taskId)
  if (task) {
    manualTaskId.value = ''
    templateId.value = String(task.templateId)
  }
})
watch(templateId, () => {
  if (manualTaskId.value && String(selectedManualTask.value?.templateId) !== templateId.value) manualTaskId.value = ''
  if (importTaskId.value && String(selectedImportTask.value?.templateId) !== templateId.value) importTaskId.value = ''
})
function addRow() { manualRows.value.push(newRow()) }
function removeRow(index: number) { if (manualRows.value.length > 1) manualRows.value.splice(index, 1) }
async function chooseFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0] || null
  previewSheets.value = []
  message.value = ''
  error.value = ''
  if (file.value && importMode.value === 'auto') await previewWorkbook()
}
async function download() { if (!selectedTemplate.value) return; try { await downloadTemplate(selectedTemplate.value.id, selectedTemplate.value.name) } catch (e) { error.value = msg(e) } }
async function previewWorkbook() {
  if (!file.value) { error.value = '请选择 Excel 文件'; return }
  error.value = ''; message.value = ''; previewing.value = true
  try {
    const result = await importPreview(file.value)
    previewSheets.value = result.sheets.map((sheet) => ({ ...sheet, templateId: sheet.suggestedTemplateId ? String(sheet.suggestedTemplateId) : '' }))
  } catch (e) { error.value = msg(e) } finally { previewing.value = false }
}
watch(importMode, (mode) => { if (mode === 'auto' && file.value && !previewSheets.value.length) previewWorkbook() })
async function submitImport() {
  if (importMode.value === 'manual' && !templateId.value) { error.value = '手工指定模式下请选择填报模板'; return }
  if (!file.value) { error.value = '请选择填写完成的 Excel 文件'; return }
  if (importMode.value === 'auto') {
    if (!previewSheets.value.length) { await previewWorkbook(); return }
    if (previewSheets.value.some((sheet) => !sheet.templateId)) { error.value = '请为每个工作表选择模板后再确认导入'; return }
  }
  error.value = ''; message.value = ''; uploading.value = true
  try {
    const result = importMode.value === 'auto' ? await confirmImport(file.value, previewSheets.value.map((sheet) => sheet.templateId)) : await importReport(templateId.value, file.value, importTaskId.value || undefined)
    message.value = `导入批次 ${result.batchId} 已完成：成功 ${result.importedRows} 行，失败 ${result.failedRows} 行`; file.value = null; previewSheets.value = []; importBatches.value = await listImportBatches()
  } catch (e) { error.value = msg(e) } finally { uploading.value = false }
}
async function submitManual() {
  if (!templateId.value || !selectedTemplate.value) { error.value = '请选择填报模板'; return }
  if (!manualRows.value.length) { error.value = '请先新增填报行'; return }
  const required = selectedTemplate.value.columns?.filter((column) => column.required) || []
  const invalid = manualRows.value.some((row) => required.some((column) => !String(row[column.key] ?? '').trim()))
  if (invalid) { error.value = '请填写每一行的必填字段'; return }
  error.value = ''; message.value = ''; saving.value = true
  try { await createReports(manualRows.value.map((row) => ({ templateId: templateId.value, taskId: manualTaskId.value || undefined, data: row, status: 'SUBMITTED' }))); message.value = `已提交 ${manualRows.value.length} 行填报数据`; resetManualRows() } catch (e) { error.value = msg(e) } finally { saving.value = false }
}
function msg(e: unknown) { return e instanceof Error ? e.message : '操作失败' }
function date(value?: string) { return value ? value.replace('T', ' ').replace(/\.\d+$/, '') : '-' }
onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>数据填报</h1><p>可下载模板导入，也可在动态表格中连续录入多行数据。</p></div></div>
    <div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div>
    <div class="import-panel">
      <div class="step"><span>1</span><div><h2>选择导入模式</h2><div class="mode-options"><label class="mode-option" :class="{ active: importMode === 'manual' }"><input v-model="importMode" type="radio" value="manual" /> <span><strong>指定单个模板</strong><small>选择模板后，所有非空工作表按同一模板校验</small></span></label><label class="mode-option" :class="{ active: importMode === 'auto' }"><input v-model="importMode" type="radio" value="auto" /> <span><strong>按工作表自动匹配</strong><small>一个 Excel 可包含多个工作表，每个工作表对应一个模板</small></span></label></div><div v-if="importMode === 'manual'" class="template-picker"><select v-model="importTaskId" class="task-select"><option value="">不关联填报任务（可选）</option><option v-for="task in publishedTasks" :key="task.id" :value="String(task.id)">{{ task.name }}{{ task.periodLabel ? `（${task.periodLabel}）` : '' }} - {{ task.templateName || '关联模板' }}</option></select><select v-model="templateId" :disabled="Boolean(importTaskId || manualTaskId)"><option value="">请选择模板</option><option v-for="item in templates" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select><button v-if="selectedTemplate" type="button" class="download" @click="download">下载{{ selectedTemplate.name }}模板</button><p v-if="selectedTemplate" class="hint">仅显示已发布任务；选择任务后将自动锁定其关联模板。文件名与模板名称一致，导入表头必须与模板完全一致。</p></div><p v-else class="hint auto-hint">无需选择模板。请确保每个工作表名称或表头能唯一匹配已启用模板；多工作表导入不关联单个填报任务。</p></div></div>
      <div class="step"><span>2</span><div><h2>Excel 导入</h2><input type="file" accept=".xlsx,.xls" @change="chooseFile" /><p class="hint">自动匹配模式选择文件后会识别非空工作表及其顺序，请确认每个工作表对应的模板名称后再提交。</p><div v-if="importMode === 'auto' && previewSheets.length" class="sheet-preview"><div class="preview-title">识别到 {{ previewSheets.length }} 个非空工作表，导入顺序如下</div><table><thead><tr><th>顺序</th><th>工作表</th><th>建议匹配</th><th>导入模板</th></tr></thead><tbody><tr v-for="sheet in previewSheets" :key="sheet.sheetIndex"><td>{{ sheet.sheetOrder + 1 }}</td><td>{{ sheet.sheetName }}</td><td>{{ sheet.matchStatus === 'NAME' ? '名称匹配' : sheet.matchStatus === 'HEADER' ? '表头匹配' : sheet.matchStatus === 'AMBIGUOUS' ? '匹配不唯一' : '未匹配' }}<small v-if="sheet.suggestedTemplateName">建议：{{ sheet.suggestedTemplateName }}</small></td><td><select v-model="sheet.templateId"><option value="">请选择模板</option><option v-for="item in templates" :key="item.id" :value="String(item.id)">{{ item.name }}</option></select></td></tr></tbody></table><p class="hint">可手动调整模板。请按上表顺序确认，顺序与实际导入顺序一致。</p></div><button class="primary wide" :disabled="uploading || previewing" @click="submitImport">{{ uploading ? '导入中...' : previewing ? '识别中...' : importMode === 'auto' && previewSheets.length ? '确认导入' : importMode === 'auto' ? '识别工作表' : '导入并提交' }}</button></div></div>
      <div class="step"><span>3</span><div><div class="manual-heading"><div><h2>手工填报</h2><p class="hint">每一行代表一条填报记录，可连续新增多行。</p></div><button v-if="selectedTemplate?.columns?.length" type="button" class="secondary" @click="addRow">新增一行</button></div><label class="task-field">填报任务（可选）<select v-model="manualTaskId"><option value="">不关联填报任务</option><option v-for="task in publishedTasks" :key="task.id" :value="String(task.id)">{{ task.name }}{{ task.periodLabel ? `（${task.periodLabel}）` : '' }} - {{ task.templateName || '关联模板' }}</option></select></label><p v-if="manualTaskId" class="hint">已根据所选已发布任务自动切换到对应模板。</p><p v-if="!selectedTemplate" class="hint">选择模板后显示填报表格。</p><div v-else-if="selectedTemplate.columns?.length" class="manual-table-wrap"><table class="manual-table"><thead><tr><th>#</th><th v-for="column in selectedTemplate.columns" :key="column.key">{{ column.label }}<span v-if="column.required"> *</span></th><th>操作</th></tr></thead><tbody><tr v-for="(row, rowIndex) in manualRows" :key="rowIndex"><td>{{ rowIndex + 1 }}</td><td v-for="column in selectedTemplate.columns" :key="column.key"><textarea v-if="column.type === 'textarea'" v-model="row[column.key]" rows="1" /><input v-else v-model="row[column.key]" :type="column.type === 'number' ? 'number' : column.type === 'date' ? 'date' : column.type === 'month' ? 'month' : 'text'" /></td><td><button type="button" class="danger-button" :disabled="manualRows.length === 1" @click="removeRow(rowIndex)">删除</button></td></tr></tbody></table><button class="primary wide" :disabled="saving" @click="submitManual">{{ saving ? '提交中...' : `提交 ${manualRows.length} 行` }}</button></div><p v-else class="hint">该模板暂无字段，请联系模板管理员维护表头。</p></div></div>
      <div class="step"><span>4</span><div><h2>导入批次</h2><p class="hint">仅展示当前用户可查看的批次；失败批次可下载错误清单。</p><div class="table-wrap"><table><thead><tr><th>文件</th><th>时间</th><th>成功/失败</th><th>状态</th><th>摘要</th><th>操作</th></tr></thead><tbody><tr v-if="!importBatches.length"><td colspan="6" class="muted">暂无导入批次。</td></tr><tr v-for="batch in importBatches" :key="batch.id"><td>{{ batch.originalFileName }}</td><td>{{ date(batch.createdAt) }}</td><td>{{ batch.importedRows }} / {{ batch.failedRows }}</td><td>{{ batch.status }}</td><td>{{ batch.summary || '-' }}</td><td><button v-if="batch.failedRows > 0" type="button" class="text-button" @click="downloadImportErrors(batch.id)">下载错误</button><span v-else>-</span></td></tr></tbody></table></div></div></div>
    </div>
  </section>
</template>
