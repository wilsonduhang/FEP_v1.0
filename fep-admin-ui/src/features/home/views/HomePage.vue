<template>
  <div class="home-page">
    <div class="top-bar">
      <TodoCountBadge ref="todoBadgeRef" />
      <el-button
        type="primary"
        size="small"
        @click="refreshAll"
      >
        刷新
      </el-button>
    </div>

    <StatsCards ref="statsCardsRef" />

    <TrendChart ref="trendChartRef" />

    <el-row
      :gutter="16"
      class="bottom-row"
    >
      <el-col :span="8">
        <BusinessDistributionChart ref="businessChartRef" />
      </el-col>
      <el-col :span="8">
        <StatusDistributionChart ref="statusChartRef" />
      </el-col>
      <el-col :span="8">
        <ShortcutGrid ref="shortcutGridRef" />
      </el-col>
    </el-row>

    <TodoList
      ref="todoListRef"
      class="todo-list"
      @update="todoBadgeRef?.load()"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import TodoCountBadge from '@/features/home/components/TodoCountBadge.vue';
import TodoList from '@/features/home/components/TodoList.vue';
import StatsCards from '@/features/home/components/StatsCards.vue';
import TrendChart from '@/features/home/components/TrendChart.vue';
import BusinessDistributionChart from '@/features/home/components/BusinessDistributionChart.vue';
import StatusDistributionChart from '@/features/home/components/StatusDistributionChart.vue';
import ShortcutGrid from '@/features/home/components/ShortcutGrid.vue';
import { useAutoRefresh } from '@/features/home/composables/useAutoRefresh';

const todoBadgeRef = ref<InstanceType<typeof TodoCountBadge> | null>(null);
const todoListRef = ref<InstanceType<typeof TodoList> | null>(null);
const statsCardsRef = ref<InstanceType<typeof StatsCards> | null>(null);
const trendChartRef = ref<InstanceType<typeof TrendChart> | null>(null);
const businessChartRef = ref<InstanceType<typeof BusinessDistributionChart> | null>(null);
const statusChartRef = ref<InstanceType<typeof StatusDistributionChart> | null>(null);
const shortcutGridRef = ref<InstanceType<typeof ShortcutGrid> | null>(null);

async function loadAll() {
  await Promise.all([
    todoBadgeRef.value?.load(),
    todoListRef.value?.load(),
    statsCardsRef.value?.load(),
    trendChartRef.value?.load(),
    businessChartRef.value?.load(),
    statusChartRef.value?.load(),
    shortcutGridRef.value?.load(),
  ]);
}

const controller = useAutoRefresh(loadAll, 30000);

async function refreshAll() {
  await controller.refresh();
}

onMounted(() => controller.start());
</script>

<style scoped>
.home-page { padding: 16px; }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.bottom-row { margin-top: 16px; }
.todo-list { margin-top: 16px; }
</style>
