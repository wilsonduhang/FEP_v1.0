<template>
  <el-card>
    <template #header>
      业务类型分布
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
import { statsApi, type DistributionItem } from '@/features/home/api/stats-api';

use([CanvasRenderer, PieChart, LegendComponent, TooltipComponent]);

const data = ref<DistributionItem[]>([]);

const option = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', right: 10 },
  series: [
    {
      type: 'pie',
      radius: '70%',
      data: data.value.map((d) => ({ name: d.messageName, value: d.count })),
    },
  ],
}));

async function load() {
  try {
    data.value = await statsApi.getDistribution();
  } catch {
    data.value = [];
  }
}

defineExpose({ load });
onMounted(load);
</script>
