<template>
  <el-dialog
    :model-value="modelValue"
    title="§3.1.2 批量生成 [Task 6 placeholder]"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <!-- Task 6 replaces this placeholder with the real batch-generate form. -->
    <p>批量生成表单将在 Task 6 中实现（节点 ID: {{ nodeId || '-' }}）。</p>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * Placeholder for {@code TlqQueueBatchGenerateDialog} — Task 6 will implement
 * the real batch-generate form per PRD §3.1.2 (9 standard queues by
 * organizationCode).
 *
 * <p>Contract is fixed now so Task 4's {@code TlqQueuesPage} integration is
 * stable:</p>
 * <ul>
 *   <li>{@code modelValue}: dialog visibility (v-model).</li>
 *   <li>{@code nodeId}: the selected node's id, passed by parent on open.</li>
 *   <li>{@code @success}: emitted after batch generation completes.</li>
 * </ul>
 */

interface Props {
  modelValue: boolean;
  nodeId: string;
}

defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();
</script>
