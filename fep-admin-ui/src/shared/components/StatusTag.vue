<template>
  <el-tag :type="resolved.type">
    {{ resolved.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export type ElTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger';
export interface StatusMapping { label: string; type: ElTagType }

const props = defineProps<{
  value: string;
  mapping: Record<string, StatusMapping>;
}>();

const resolved = computed<StatusMapping>(() =>
  props.mapping[props.value] ?? { label: props.value, type: 'info' },
);
</script>
