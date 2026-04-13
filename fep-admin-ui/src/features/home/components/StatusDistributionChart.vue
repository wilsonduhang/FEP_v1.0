<template>
  <el-card>
    <template #header>
      报文状态占比
    </template>
    <v-chart
      :option="option"
      autoresize
      style="height: 320px; width: 100%"
    />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import {
  statsApi,
  type StatusDistributionItem,
} from '@/features/home/api/stats-api';

use([CanvasRenderer, PieChart, LegendComponent, TooltipComponent]);

const data = ref<StatusDistributionItem[]>([]);

const option = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', right: 10 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      data: data.value.map((d) => ({ name: d.status, value: d.count })),
    },
  ],
}));

async function load() {
  try {
    data.value = await statsApi.getStatusDistribution();
  } catch {
    data.value = [];
  }
}

defineExpose({ load });
onMounted(load);
</script>
