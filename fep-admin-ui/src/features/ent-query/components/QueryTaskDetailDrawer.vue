<template>
  <DetailDrawer
    :model-value="modelValue"
    :title="'任务详情 — ' + taskId"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p>待 Task 4 实现</p>
  </DetailDrawer>
</template>

<script setup lang="ts">
import { DetailDrawer } from '@/shared/components';

defineProps<{ modelValue: boolean; taskId: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();
</script>
