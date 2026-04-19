<!-- src/features/submission/components/ViewTrendChart.vue -->
<template>
  <div class="view-trend-chart">
    <div class="chart-header">
      <h3>趋势（区间趋势）</h3>
    </div>
    <div v-if="points.length === 0" class="empty">暂无数据</div>
    <div v-else ref="chartRef" class="chart" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { TrendPoint } from '../api/sub-report-api';

/**
 * Trend chart for §5.6.3 by-type view page.
 *
 * <p>Renders a Bar chart with X axis = period (YYYY-MM) and Y axis = count.
 * Uses echarts on-demand imports (Canvas + Grid + Tooltip) to avoid pulling
 * the full echarts bundle (P7.1 convention).</p>
 *
 * <p>Displays "暂无数据" placeholder when {@code points} is empty; otherwise
 * initialises the instance lazily inside {@code nextTick} so the ref is
 * already attached when echarts resolves the container size.</p>
 */
echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

const props = defineProps<{ points: TrendPoint[] }>();
const chartRef = ref<HTMLDivElement>();
let instance: echarts.ECharts | null = null;

function render(): void {
  if (!chartRef.value) return;
  if (!instance) instance = echarts.init(chartRef.value);
  instance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '40', right: '20', bottom: '40', top: '20' },
    xAxis: { type: 'category', data: props.points.map((p) => p.period) },
    yAxis: { type: 'value', name: '数据量' },
    series: [
      {
        type: 'bar',
        data: props.points.map((p) => p.count),
        itemStyle: { color: '#409eff' },
      },
    ],
  });
}

onMounted(() => nextTick(render));
watch(() => props.points, () => nextTick(render), { deep: true });
onBeforeUnmount(() => {
  instance?.dispose();
  instance = null;
});
</script>

<style scoped>
.view-trend-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.chart-header h3 {
  margin: 0;
  font-size: 16px;
}
.chart {
  width: 100%;
  height: 320px;
}
.empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}
</style>
