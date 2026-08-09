<script setup lang="ts">
import type { Department } from '../api/departments'

defineProps<{ department: Department; childrenByParent: Map<string, Department[]> }>()
const emit = defineEmits<{ edit: [department: Department]; remove: [department: Department] }>()
</script>

<template>
  <div class="tree-node-row"><span>{{ department.name }}</span><span class="tree-actions"><button class="text-button" @click="emit('edit', department)">编辑</button><button class="danger-button" @click="emit('remove', department)">删除</button></span></div>
  <ul v-if="childrenByParent.get(String(department.id))?.length" class="tree-list"><li v-for="child in childrenByParent.get(String(department.id))" :key="child.id"><DepartmentTreeNode :department="child" :children-by-parent="childrenByParent" @edit="emit('edit', $event)" @remove="emit('remove', $event)" /></li></ul>
</template>
