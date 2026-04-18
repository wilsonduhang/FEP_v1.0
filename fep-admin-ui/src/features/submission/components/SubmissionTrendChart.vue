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
import { LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import type { DashboardTrend } from '../api/sub-dashboard-api';

use([CanvasRenderer, LineChart, GridComponent, LegendComponent, TooltipComponent]);

/**
 * Submission push trend chart (Line chart).
 *
 * <p>Renders two series (已推送 / 待推送) against a daily category axis.
 * Chart data is entirely driven by the parent via the {@code data} prop
 * so the page composable can batch-refresh all dashboard widgets together.</p>
 */
const props = defineProps<{ data: DashboardTrend }>();

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['已推送', '待推送'] },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: props.data.dates },
  yAxis: { type: 'value' },
  series: [
    { name: '已推送', type: 'line', smooth: true, data: props.data.pushedCounts },
    { name: '待推送', type: 'line', smooth: true, data: props.data.pendingCounts },
  ],
}));
</script>
