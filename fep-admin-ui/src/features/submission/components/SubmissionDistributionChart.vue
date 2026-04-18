<template>
  <v-chart
    :option="option"
    autoresize
    style="height: 320px; width: 100%"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import type { DashboardDistributionItem } from '../api/sub-dashboard-api';

use([CanvasRenderer, PieChart, LegendComponent, TooltipComponent]);

/**
 * Submission distribution chart (Pie chart).
 *
 * <p>Renders a single Top-10 pie whose slices come from
 * {@link DashboardDistributionItem#name} + {@link DashboardDistributionItem#value}.
 * Data is parent-driven so the page can switch between
 * {@code messageType} / {@code businessType} dimensions.</p>
 */
const props = defineProps<{ data: DashboardDistributionItem[] }>();

const option = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', right: 10 },
  series: [
    {
      type: 'pie',
      radius: '60%',
      data: props.data.map((d) => ({ name: d.name, value: d.value })),
    },
  ],
}));
</script>
