<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createDepartment, deleteDepartment, listDepartments, updateDepartment, type Department } from '../api/departments'
import DepartmentTreeNode from './DepartmentTreeNode.vue'

const departments = ref<Department[]>([])
const editing = ref<Department | null>(null)
const form = ref({ name: '', parentId: '' })
const error = ref('')
const message = ref('')
const childrenByParent = computed(() => {
  const result = new Map<string, Department[]>()
  for (const department of departments.value) {
    const key = department.parentId == null ? 'root' : String(department.parentId)
    result.set(key, [...(result.get(key) || []), department].sort((left, right) => left.name.localeCompare(right.name)))
  }
  return result
})
const roots = computed(() => childrenByParent.value.get('root') || [])

function text(error: unknown) { return error instanceof Error ? error.message : '操作失败' }
function descendants(id: Department['id']): Set<string> {
  const result = new Set<string>([String(id)])
  const visit = (parentId: string) => {
    for (const child of childrenByParent.value.get(parentId) || []) {
      result.add(String(child.id))
      visit(String(child.id))
    }
  }
  visit(String(id))
  return result
}
function parentOptions() {
  const excluded = editing.value ? descendants(editing.value.id) : new Set<string>()
  return departments.value.filter((department) => !excluded.has(String(department.id)))
}
function reset() { editing.value = null; form.value = { name: '', parentId: '' } }
function edit(department: Department) { editing.value = department; form.value = { name: department.name, parentId: department.parentId == null ? '' : String(department.parentId) } }
async function load() { try { departments.value = await listDepartments() } catch (caught) { error.value = text(caught) } }
async function save() {
  if (!form.value.name.trim()) { error.value = '部门名称不能为空'; return }
  try {
    const data = { name: form.value.name.trim(), parentId: form.value.parentId ? Number(form.value.parentId) : null }
    if (editing.value) await updateDepartment(editing.value.id, data); else await createDepartment(data)
    message.value = editing.value ? '部门已更新' : '部门已创建'
    reset()
    await load()
  } catch (caught) { error.value = text(caught) }
}
async function remove(department: Department) {
  if (!window.confirm(`确认删除部门“${department.name}”吗？存在子部门、用户或任务引用时系统会阻止删除。`)) return
  try { await deleteDepartment(department.id); message.value = '部门已删除'; await load() } catch (caught) { error.value = text(caught) }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>部门管理</h1><p>维护部门树，删除被用户或任务引用的部门会被阻止。</p></div></div>
    <div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div>
    <form class="editor" @submit.prevent="save"><h2>{{ editing ? '编辑部门' : '新增部门' }}</h2><div class="form-grid"><label>部门名称<input v-model.trim="form.name" maxlength="100" /></label><label>上级部门<select v-model="form.parentId"><option value="">顶级部门</option><option v-for="department in parentOptions()" :key="department.id" :value="String(department.id)">{{ department.name }}</option></select></label></div><div class="actions"><button class="primary">保存</button><button v-if="editing" type="button" class="secondary" @click="reset">取消</button></div></form>
    <div class="department-tree"><p v-if="!roots.length" class="muted">暂无部门</p><ul v-else class="tree-list"><li v-for="department in roots" :key="department.id"><DepartmentTreeNode :department="department" :children-by-parent="childrenByParent" @edit="edit" @remove="remove" /></li></ul></div>
  </section>
</template>
