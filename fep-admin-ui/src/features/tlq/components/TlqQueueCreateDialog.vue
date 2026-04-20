<template>
  <el-dialog
    :model-value="modelValue"
    title="新建队列 [Task 5 placeholder]"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <!-- Task 5 replaces this placeholder with the real queue create form. -->
    <p>队列创建表单将在 Task 5 中实现（节点 ID: {{ nodeId || '-' }}）。</p>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * Placeholder for {@code TlqQueueCreateDialog} — Task 5 will implement the
 * real create form (queueName/channelType/queueType/description + submit).
 *
 * <p>Contract is fixed now so Task 4's {@code TlqQueuesPage} integration is
 * stable:</p>
 * <ul>
 *   <li>{@code modelValue}: dialog visibility (v-model).</li>
 *   <li>{@code nodeId}: the selected node's id, passed by parent on open.</li>
 *   <li>{@code @success}: emitted after a successful create so parent reloads.</li>
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
