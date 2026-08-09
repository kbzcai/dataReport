<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createUser, deleteUser, listUsers, updateUser, type ManagedUser } from '../api/users'
import { listDepartments, type Department } from '../api/departments'

const users = ref<ManagedUser[]>([])
const departments = ref<Department[]>([])
const form = ref({ username: '', password: '', roles: ['REPORTER'] as string[], departmentId: '', permissions: [] as string[] })
const error = ref('')
const message = ref('')
const loading = ref(false)
const selectedRoles = ref<Record<string, string[]>>({})
const selectedPermissions = ref<Record<string, string[]>>({})
const selectedDepartments = ref<Record<string, string>>({})
const roles = [
  { value: 'ADMIN', label: '系统管理员' },
  { value: 'MAINTAINER', label: '模板管理员' },
  { value: 'LEADER', label: '数据领导' },
  { value: 'REPORTER', label: '填报人员' },
]
const permissions = [
  { value: 'REPORT_VIEW', label: '查看填报数据' },
  { value: 'REPORT_EDIT', label: '编辑填报数据' },
]
function text(error: unknown) { return error instanceof Error ? error.message : '操作失败' }
function rolesOf(user: ManagedUser) { return user.roles?.length ? user.roles.map(String) : user.role ? [String(user.role)] : [] }
function permissionsOf(user: ManagedUser) { return (user.permissions || []).map(String) }
function departmentLabel(department: Department) {
  const parts = [department.name]
  let parentId = department.parentId
  while (parentId != null) {
    const parent = departments.value.find((item) => String(item.id) === String(parentId))
    if (!parent) break
    parts.unshift(parent.name)
    parentId = parent.parentId
  }
  return parts.join(' / ')
}
function userRoleLabel(user: ManagedUser) { return rolesOf(user).map((value) => roles.find((role) => role.value === value)?.label || value).join('、') || '-' }

async function load() {
  loading.value = true
  try {
    users.value = await listUsers()
    selectedRoles.value = Object.fromEntries(users.value.map((user) => [String(user.id), rolesOf(user)]))
    selectedPermissions.value = Object.fromEntries(users.value.map((user) => [String(user.id), permissionsOf(user)]))
    selectedDepartments.value = Object.fromEntries(users.value.map((user) => [String(user.id), user.departmentId == null ? '' : String(user.departmentId)]))
  } catch (caught) { error.value = text(caught) } finally { loading.value = false }
}
async function save() {
  if (!form.value.username || !form.value.password) { error.value = '账号和初始密码不能为空'; return }
  if (!form.value.roles.length) { error.value = '请至少选择一个业务角色'; return }
  error.value = ''
  try {
    await createUser({ username: form.value.username, password: form.value.password, roles: form.value.roles, permissions: form.value.permissions, departmentId: form.value.departmentId ? Number(form.value.departmentId) : null })
    message.value = '用户已创建'
    form.value = { username: '', password: '', roles: ['REPORTER'], departmentId: '', permissions: [] }
    await load()
  } catch (caught) { error.value = text(caught) }
}
async function saveUser(user: ManagedUser) {
  const id = String(user.id)
  if (!selectedRoles.value[id]?.length) { error.value = `用户 ${user.username} 至少需要一个业务角色`; return }
  try {
    await updateUser(user.id, {
      roles: selectedRoles.value[id],
      permissions: selectedPermissions.value[id] || [],
      departmentId: selectedDepartments.value[id] ? Number(selectedDepartments.value[id]) : null,
      departmentProvided: true,
    })
    message.value = '用户角色、权限与部门已更新'
    await load()
  } catch (caught) { error.value = text(caught) }
}
async function remove(user: ManagedUser) {
  if (!window.confirm(`确认删除用户“${user.username}”吗？`)) return
  try { await deleteUser(user.id); message.value = '用户已删除'; await load() } catch (caught) { error.value = text(caught) }
}
async function toggle(user: ManagedUser) {
  try { await updateUser(user.id, { enabled: !user.enabled }); message.value = user.enabled ? '用户已停用' : '用户已启用'; await load() } catch (caught) { error.value = text(caught) }
}

onMounted(async () => {
  try { departments.value = await listDepartments() } catch (caught) { error.value = text(caught) }
  await load()
})
</script>

<template>
  <section>
    <div class="page-heading"><div><h1>权限管理</h1><p>业务角色、数据权限和部门归属分别维护。</p></div></div>
    <div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div>
    <form class="editor" @submit.prevent="save">
      <h2>新增用户</h2>
      <div class="form-grid">
        <label>登录账号<input v-model.trim="form.username" maxlength="64" /></label>
        <label>初始密码<input v-model="form.password" type="password" maxlength="100" /></label>
        <label>所属部门<select v-model="form.departmentId"><option value="">未分配</option><option v-for="department in departments" :key="department.id" :value="String(department.id)">{{ departmentLabel(department) }}</option></select></label>
      </div>
      <fieldset class="choice-set"><legend>业务角色</legend><label v-for="role in roles" :key="role.value" class="checkbox-label"><input v-model="form.roles" type="checkbox" :value="role.value" />{{ role.label }}</label></fieldset>
      <fieldset class="choice-set"><legend>数据权限</legend><label v-for="permission in permissions" :key="permission.value" class="checkbox-label"><input v-model="form.permissions" type="checkbox" :value="permission.value" />{{ permission.label }}</label></fieldset>
      <div class="actions"><button class="primary">创建用户</button></div>
    </form>
    <div class="table-wrap"><table><thead><tr><th>账号</th><th>角色</th><th>权限</th><th>部门</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr v-if="loading"><td colspan="6" class="muted">加载中...</td></tr><tr v-else-if="!users.length"><td colspan="6" class="muted">暂无用户</td></tr>
      <tr v-for="user in users" :key="user.id"><td>{{ user.username }}</td>
        <td><div class="compact-choices"><label v-for="role in roles" :key="role.value"><input v-model="selectedRoles[String(user.id)]" type="checkbox" :value="role.value" />{{ role.label }}</label></div><small class="hint">当前：{{ userRoleLabel(user) }}</small></td>
        <td><div class="compact-choices"><label v-for="permission in permissions" :key="permission.value"><input v-model="selectedPermissions[String(user.id)]" type="checkbox" :value="permission.value" />{{ permission.label }}</label></div></td>
        <td><select v-model="selectedDepartments[String(user.id)]"><option value="">未分配</option><option v-for="department in departments" :key="department.id" :value="String(department.id)">{{ departmentLabel(department) }}</option></select></td>
        <td>{{ user.enabled ? '启用' : '停用' }}</td><td class="actions-cell"><button class="text-button" @click="saveUser(user)">保存</button><button class="text-button" @click="toggle(user)">{{ user.enabled ? '停用' : '启用' }}</button><button class="danger-button" @click="remove(user)">删除</button></td>
      </tr>
    </tbody></table></div>
  </section>
</template>
