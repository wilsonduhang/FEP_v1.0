<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    :size="size"
    direction="rtl"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <slot />
    <template
      v-if="$slots.footer"
      #footer
    >
      <slot name="footer" />
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: boolean;
  title: string;
  size?: string;
}>(), { size: '50%' });

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();
</script>
