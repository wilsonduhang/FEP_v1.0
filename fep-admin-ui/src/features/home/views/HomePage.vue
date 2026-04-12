<template>
  <div class="home-shell">
    <h2>欢迎，{{ userName }}</h2>
    <p class="hint">
      P7.0 首页壳 — 数据看板、待办事项、快捷入口将在 P7.1 实现。
    </p>
    <p class="time">
      当前时间：{{ now }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const userName = computed(() => authStore.profile?.userName ?? '访客');
const now = ref('');
let timer: ReturnType<typeof setInterval> | null = null;

function tick() {
  now.value = new Date().toLocaleString('zh-CN', { hour12: false });
}

onMounted(() => {
  tick();
  timer = setInterval(tick, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.home-shell { padding: 24px; }
.hint { color: #888; }
.time { font-family: monospace; margin-top: 16px; }
</style>
