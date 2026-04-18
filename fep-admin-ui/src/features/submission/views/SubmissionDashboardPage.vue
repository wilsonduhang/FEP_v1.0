<template>
  <div class="dashboard-page">
    <div class="header">
      <h2>数据概况</h2>
      <el-button
        type="primary"
        size="small"
        data-test="manual-refresh"
        @click="handleManualRefresh"
      >
        手动刷新
      </el-button>
    </div>

    <el-row
      :gutter="16"
      class="cards"
    >
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            总接口数
          </div>
          <div
            class="card-value"
            data-test="card-totalInterfaceCount"
          >
            {{ overview ? overview.totalInterfaceCount : '-' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            启用接口
          </div>
          <div
            class="card-value"
            data-test="card-enabledInterfaceCount"
          >
            {{ overview ? overview.enabledInterfaceCount : '-' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            总数据源
          </div>
          <div
            class="card-value"
            data-test="card-totalDataSourceCount"
          >
            {{ overview ? overview.totalDataSourceCount : '-' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            总报送记录
          </div>
          <div
            class="card-value"
            data-test="card-totalRecordCount"
          >
            {{ overview ? overview.totalRecordCount : '-' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            已推送
          </div>
          <div
            class="card-value"
            data-test="card-pushedRecordCount"
          >
            {{ overview ? overview.pushedRecordCount : '-' }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card>
          <div class="card-label">
            待推送
          </div>
          <div
            class="card-value"
            data-test="card-pendingRecordCount"
          >
            {{ overview ? overview.pendingRecordCount : '-' }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="chart-section">
      <template #header>
        <div class="chart-header">
          <span>推送趋势</span>
          <el-radio-group
            v-model="trendDays"
            size="small"
            @change="handleTrendDaysChange"
          >
            <el-radio-button :value="7">
              7 日
            </el-radio-button>
            <el-radio-button :value="30">
              30 日
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <SubmissionTrendChart :data="trend" />
    </el-card>

    <el-card class="chart-section">
      <template #header>
        <div class="chart-header">
          <span>分布统计</span>
          <el-radio-group
            v-model="distDim"
            size="small"
            @change="handleDistDimChange"
          >
            <el-radio-button value="messageType">
              报文类型
            </el-radio-button>
            <el-radio-button value="businessType">
              业务类型
            </el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <SubmissionDistributionChart :data="distribution" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useAutoRefresh } from '@/features/home/composables/useAutoRefresh';
import {
  subDashboardApi,
  type DashboardOverview,
  type DashboardTrend,
  type DashboardDistributionItem,
  type DashboardTrendDays,
  type DashboardDistributionDim,
} from '../api/sub-dashboard-api';
import SubmissionTrendChart from '../components/SubmissionTrendChart.vue';
import SubmissionDistributionChart from '../components/SubmissionDistributionChart.vue';

/**
 * Submission Dashboard page (PRD §5.5.1 / FR-WEB-SUB-DASH / FR-WEB-SUB-API).
 *
 * <p>Consumes three endpoints exposed by Task 0b:</p>
 * <ul>
 *   <li>{@code GET /dashboard} — 6 aggregated overview cards</li>
 *   <li>{@code GET /dashboard/trend?days=7|30} — Line chart</li>
 *   <li>{@code GET /dashboard/distribution?dim=messageType|businessType} — Pie chart</li>
 * </ul>
 *
 * <p>Auto-refresh every 30 s via {@link useAutoRefresh}; {@code ctrl.start()}
 * runs once on mount (composable does NOT auto-start). {@code ctrl.stop()} is
 * wired by the composable on unmount.</p>
 */

const overview = ref<DashboardOverview>();
const trend = ref<DashboardTrend>({ dates: [], pushedCounts: [], pendingCounts: [] });
const distribution = ref<DashboardDistributionItem[]>([]);
const trendDays = ref<DashboardTrendDays>(7);
const distDim = ref<DashboardDistributionDim>('messageType');

async function loadAll(): Promise<void> {
  try {
    const [ov, tr, dist] = await Promise.all([
      subDashboardApi.getOverview(),
      subDashboardApi.getTrend(trendDays.value),
      subDashboardApi.getDistribution(distDim.value),
    ]);
    overview.value = ov;
    trend.value = tr;
    distribution.value = dist;
  } catch {
    // Keep prior values visible; surface failure via toast instead of unhandled
    // rejection (useAutoRefresh invokes `void loader()` which swallows errors).
    ElMessage.error('数据概况加载失败');
  }
}

const ctrl = useAutoRefresh(loadAll, 30_000);

async function handleManualRefresh(): Promise<void> {
  await ctrl.refresh();
}

async function handleTrendDaysChange(value: DashboardTrendDays): Promise<void> {
  // `v-model` on el-radio-group already mutated `trendDays` before @change fires,
  // so we use `value` directly in the API call without re-assigning the ref.
  try {
    trend.value = await subDashboardApi.getTrend(value);
  } catch {
    ElMessage.error('趋势数据加载失败');
  }
}

async function handleDistDimChange(value: DashboardDistributionDim): Promise<void> {
  // Same rationale as handleTrendDaysChange: v-model already updated `distDim`.
  try {
    distribution.value = await subDashboardApi.getDistribution(value);
  } catch {
    ElMessage.error('分布数据加载失败');
  }
}

onMounted(() => ctrl.start());

defineExpose({
  loadAll,
  handleManualRefresh,
  handleTrendDaysChange,
  handleDistDimChange,
});
</script>

<style scoped>
.dashboard-page { padding: 24px; }
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header h2 { margin: 0; }
.cards { margin-bottom: 24px; }
.card-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 8px;
}
.card-value {
  font-size: 24px;
  font-weight: 600;
}
.chart-section { margin-bottom: 24px; }
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
