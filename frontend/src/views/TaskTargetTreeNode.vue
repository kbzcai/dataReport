<script setup lang="ts">
import { computed } from 'vue'
import type { AssignableTarget } from '../api/tasks'

const props = defineProps<{ target: AssignableTarget; childrenByParent: Map<string, AssignableTarget[]>; departmentIds: Array<number | string>; assigneeIds: Array<number | string> }>()
const emit = defineEmits<{ toggleDepartment: [id: number | string, checked: boolean]; toggleAssignee: [id: number | string, checked: boolean] }>()
const children = computed(() => props.childrenByParent.get(String(props.target.id)) || [])
function selected(id: number | string) { return props.assigneeIds.some((value) => String(value) === String(id)) }
function departmentSelected(id: number | string) { return props.departmentIds.some((value) => String(value) === String(id)) }
</script>

<template>
  <div class="target-node"><label class="checkbox-label"><input type="checkbox" :checked="departmentSelected(target.id)" @change="emit('toggleDepartment', target.id, ($event.target as HTMLInputElement).checked)" /><strong>{{ target.name }}</strong></label><div v-if="target.users.length" class="target-users"><label v-for="user in target.users" :key="user.id" class="checkbox-label"><input type="checkbox" :checked="selected(user.id)" @change="emit('toggleAssignee', user.id, ($event.target as HTMLInputElement).checked)" />{{ user.username }}（{{ user.roles.join('、') }}）</label></div></div>
  <ul v-if="children.length" class="tree-list"><li v-for="child in children" :key="child.id"><TaskTargetTreeNode :target="child" :children-by-parent="childrenByParent" :department-ids="departmentIds" :assignee-ids="assigneeIds" @toggle-department="(id, checked) => emit('toggleDepartment', id, checked)" @toggle-assignee="(id, checked) => emit('toggleAssignee', id, checked)" /></li></ul>
</template>
