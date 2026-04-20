<!--
  src/features/tlq/components/TlqNodeEditDialog.vue

  Placeholder stub — Task 3 (P7.2d) will replace this with the real form
  supporting create/update of TLQ nodes. Keeping a minimal el-dialog here
  so TlqNodesPage (Task 2) can import it deterministically under Vite's
  static module resolution. Do NOT add business logic here; expand in
  Task 3 per PRD §5.7.1.
-->
<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? '新建节点' : '编辑节点'"
    width="520px"
    :close-on-click-modal="false"
  >
    <!-- Task 3 provides the real form here. -->
    <div class="placeholder-body" data-role="edit-dialog-placeholder">
      节点编辑表单 — Task 3 提供
    </div>
    <template #footer>
      <el-button @click="visible = false"> 关闭 </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TlqNodeResponse } from '../types';

const props = defineProps<{
  modelValue: boolean;
  mode: 'create' | 'update';
  record: TlqNodeResponse | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
});
</script>

<style scoped>
.placeholder-body {
  padding: 24px 0;
  color: #909399;
  text-align: center;
}
</style>
