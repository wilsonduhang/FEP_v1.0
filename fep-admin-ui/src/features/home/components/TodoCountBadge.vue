<template>
  <div class="todo-count">
    待处理: <strong>{{ count }}</strong>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { todoApi } from '@/features/home/api/todo-api';

const count = ref(0);

async function load() {
  try {
    count.value = await todoApi.countPending();
  } catch {
    count.value = 0;
  }
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
.todo-count {
  padding: 8px;
}
.todo-count strong {
  color: #f56c6c;
  font-size: 18px;
}
</style>
