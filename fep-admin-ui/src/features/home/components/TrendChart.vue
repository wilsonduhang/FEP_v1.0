<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>报文趋势</span>
        <el-radio-group
          v-model="range"
          size="small"
          @change="load"
        >
          <el-radio-button label="TODAY">
            今日
          </el-radio-button>
          <el-radio-button label="THIS_WEEK">
            本周
          </el-radio-button>
          <el-radio-button label="THIS_MONTH">
            本月
          </el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <v-chart
      ref="chartRef"
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
import { LineChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components';
import VChart from 'vue-echarts';
import {
  statsApi,
  type TimeRange,
  type TrendDataPoint,
} from '@/features/home/api/stats-api';

use([
  CanvasRenderer,
  LineChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
]);

const range = ref<TimeRange>('THIS_WEEK');
const data = ref<TrendDataPoint[]>([]);

const option = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['发送', '接收'] },
  grid: { left: 40, right: 20, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: data.value.map((d) => d.label) },
  yAxis: { type: 'value' },
  series: [
    { name: '发送', type: 'line', smooth: true, data: data.value.map((d) => d.sentCount) },
    { name: '接收', type: 'line', smooth: true, data: data.value.map((d) => d.receivedCount) },
  ],
}));

async function load() {
  try {
    data.value = await statsApi.getTrend(range.value);
  } catch {
    data.value = [];
  }
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
