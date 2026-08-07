<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { createUser, deleteUser, listUsers, updateUser, type ManagedUser } from '../api/users'

const users = ref<ManagedUser[]>([])
const form = ref({ username: '', password: '', role: 'REPORTER' })
const error = ref(''); const message = ref(''); const loading = ref(false)
const selectedRoles = ref<Record<string, string>>({})
const roles = [{ value: 'MAINTAINER', label: '模板管理员' }, { value: 'LEADER', label: '数据领导' }, { value: 'REPORTER', label: '填报人员' }, { value: 'ADMIN', label: '系统管理员' }]
function text(e: unknown) { return e instanceof Error ? e.message : '操作失败' }
async function load() { loading.value = true; try { users.value = await listUsers(); selectedRoles.value = Object.fromEntries(users.value.map((user) => [String(user.id), (user.roles || [user.role])[0] || 'REPORTER'])) } catch (e) { error.value = text(e) } finally { loading.value = false } }
async function save() { if (!form.value.username || !form.value.password) { error.value = '账号和初始密码不能为空'; return }; error.value = ''; try { await createUser({ username: form.value.username, password: form.value.password, roles: [form.value.role] }); message.value = '用户已创建'; form.value = { username: '', password: '', role: 'REPORTER' }; await load() } catch (e) { error.value = text(e) } }
async function remove(user: ManagedUser) { if (!window.confirm(`确认删除用户“${user.username}”吗？`)) return; try { await deleteUser(user.id); message.value = '用户已删除'; await load() } catch (e) { error.value = text(e) } }
async function saveRole(user: ManagedUser) { try { await updateUser(user.id, { roles: [selectedRoles.value[String(user.id)]] }); message.value = '用户角色已更新'; await load() } catch (e) { error.value = text(e) } }
async function toggle(user: ManagedUser) { try { await updateUser(user.id, { enabled: !user.enabled }); message.value = user.enabled ? '用户已停用' : '用户已启用'; await load() } catch (e) { error.value = text(e) } }
onMounted(load)
</script>

<template>
  <section><div class="page-heading"><div><h1>权限管理</h1><p>维护用户账号及业务角色。</p></div></div><div v-if="message" class="notice success">{{ message }}</div><div v-if="error" class="notice error">{{ error }}</div>
    <form class="editor" @submit.prevent="save"><h2>新增用户</h2><div class="form-grid"><label>登录账号<input v-model.trim="form.username" maxlength="64" /></label><label>初始密码<input v-model="form.password" type="password" maxlength="100" /></label><label>角色<select v-model="form.role"><option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option></select></label></div><div class="actions"><button class="primary">创建用户</button></div></form>
    <div class="table-wrap"><table><thead><tr><th>账号</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-if="loading"><td colspan="4" class="muted">加载中...</td></tr><tr v-else-if="!users.length"><td colspan="4" class="muted">暂无用户</td></tr><tr v-for="user in users" :key="user.id"><td>{{ user.username }}</td><td><select v-model="selectedRoles[String(user.id)]"><option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option></select></td><td>{{ user.enabled ? '启用' : '停用' }}</td><td class="actions-cell"><button class="text-button" @click="saveRole(user)">保存角色</button><button class="text-button" @click="toggle(user)">{{ user.enabled ? '停用' : '启用' }}</button><button class="danger-button" @click="remove(user)">删除</button></td></tr></tbody></table></div>
  </section>
</template>
