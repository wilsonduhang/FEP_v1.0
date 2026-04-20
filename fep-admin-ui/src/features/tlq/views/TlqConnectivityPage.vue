<!-- src/features/tlq/views/TlqConnectivityPage.vue -->
<template>
  <div class="tlq-connectivity-page">
    <el-page-header
      title="TLQ 连通性测试"
      content="§5.7.5 9005 心跳 · 统计 · 历史"
      class="page-header"
    />

    <div class="toolbar">
      <el-select
        v-model="selectedNodeId"
        placeholder="请选择节点"
        style="width: 340px"
        filterable
        clearable
        @change="onNodeChange"
      >
        <el-option
          v-for="n in nodes"
          :key="n.nodeId"
          :value="n.nodeId"
          :label="`${n.nodeName} (${n.hostIp}:${n.port})`"
        />
      </el-select>
    </div>

    <el-empty
      v-if="!selectedNodeId"
      description="请先选择节点"
    />

    <template v-else>
      <div class="summary-cards">
        <el-card class="stat-card">
          <div class="label">
            总测试数
          </div>
          <div class="value">
            {{ summary?.totalTests ?? 0 }}
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="label">
            成功数
          </div>
          <div class="value">
            {{ summary?.successCount ?? 0 }}
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="label">
            成功率
          </div>
          <div class="value">
            {{ formatSuccessRate(summary) }}
          </div>
        </el-card>
        <el-card class="stat-card">
          <div class="label">
            最近测试
          </div>
          <div class="value">
            <StatusTag
              v-if="summary?.lastResult"
              :value="summary.lastResult"
              :mapping="CONNECTIVITY_RESULT_MAP"
            />
            <span v-else>-</span>
          </div>
          <div class="sub">
            {{ summary?.lastTestTime ?? '-' }}
          </div>
        </el-card>
      </div>

      <div class="trigger-area">
        <el-tooltip
          content="Mock Mode — TLQ SDK 就绪后启用真实 9005 心跳"
          placement="top"
        >
          <el-button
            type="primary"
            :loading="triggering"
            @click="trigger"
          >
            触发 9005 心跳测试
          </el-button>
        </el-tooltip>
        <MockBadge />
      </div>

      <el-table
        v-loading="recordsLoading"
        :data="records"
        stripe
        border
        style="margin-top: 16px;"
      >
        <el-table-column
          prop="testTime"
          label="测试时间"
          width="170"
          :formatter="formatTestTime"
        />
        <el-table-column
          label="结果"
          width="90"
        >
          <template #default="{ row }">
            <StatusTag
              :value="row.testResult"
              :mapping="CONNECTIVITY_RESULT_MAP"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="rttMs"
          label="RTT (ms)"
          width="100"
          :formatter="formatRtt"
        />
        <el-table-column
          prop="errorMessage"
          label="错误信息"
          show-overflow-tooltip
        />
        <el-table-column
          prop="triggeredBy"
          label="触发来源"
          width="120"
        />
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="recordsTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, prev, pager, next, sizes"
        style="margin-top: 16px;"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { MockBadge, StatusTag } from '@/shared/components';
import { CONNECTIVITY_RESULT_MAP } from '@/shared/types/enum-maps';
import { tlqNodeApi } from '../api/tlq-node-api';
import { tlqConnectivityApi } from '../api/tlq-connectivity-api';
import type {
  ConnectivityRecordResponse,
  ConnectivitySummaryResponse,
  TlqNodeResponse,
} from '../types';

/**
 * TLQ connectivity test page — §5.7.5 (FR-WEB-TLQ-HB).
 *
 * <p>Node-scoped connectivity dashboard. A top-level {@code el-select} picks
 * the target TLQ node; selection dispatches two parallel REST calls
 * ({@code getSummary} + {@code listRecords}) and renders four stat cards plus
 * a paginated history table. The 9005 heartbeat trigger button is currently
 * a reserved shell (mock mode) — a {@code MockBadge} marks the UI action as
 * non-production until the real TLQ SDK (P1) is wired in.</p>
 *
 * <p>Toast dispatch by trigger result: {@code SUCCESS → ElMessage.success}
 * with RTT appended when non-null; {@code TIMEOUT → ElMessage.warning};
 * {@code FAILURE → ElMessage.error}. The success-rate card guards against
 * {@code NaN} by rendering {@code "-"} when {@code totalTests === 0}.</p>
 *
 * <p>Red line #1 compliance: trigger and data-refresh actions are reached
 * through DOM button clicks or child-component emits only. {@code
 * defineExpose} narrows to state refs and the node-change boundary handler
 * required for jsdom simulation of el-select option pick; internal trigger
 * and load-*-detail handlers are never exposed.</p>
 */

const nodes = ref<TlqNodeResponse[]>([]);
const selectedNodeId = ref('');
const summary = ref<ConnectivitySummaryResponse | null>(null);
const records = ref<ConnectivityRecordResponse[]>([]);
const recordsTotal = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const recordsLoading = ref(false);
const triggering = ref(false);

function formatSuccessRate(s: ConnectivitySummaryResponse | null): string {
  if (!s || s.totalTests === 0) return '-';
  return `${s.successRate.toFixed(2)}%`;
}

function formatTestTime(row: ConnectivityRecordResponse): string {
  if (!row.testTime) return '-';
  try {
    return new Date(row.testTime).toLocaleString('zh-CN');
  } catch {
    return row.testTime;
  }
}

function formatRtt(row: ConnectivityRecordResponse): string {
  return row.rttMs !== null && row.rttMs !== undefined ? String(row.rttMs) : '-';
}

async function loadNodes(): Promise<void> {
  try {
    const result = await tlqNodeApi.listNodes({ pageNum: 1, pageSize: 1000 });
    nodes.value = result.records;
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载节点列表失败';
    ElMessage.error(msg);
  }
}

async function loadSummary(): Promise<void> {
  if (!selectedNodeId.value) return;
  try {
    summary.value = await tlqConnectivityApi.getSummary(selectedNodeId.value);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载统计失败';
    ElMessage.error(msg);
  }
}

async function loadRecords(): Promise<void> {
  if (!selectedNodeId.value) return;
  recordsLoading.value = true;
  try {
    const result = await tlqConnectivityApi.listRecords(selectedNodeId.value, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    records.value = result.records;
    recordsTotal.value = result.total;
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载历史记录失败';
    ElMessage.error(msg);
  } finally {
    recordsLoading.value = false;
  }
}

async function loadData(): Promise<void> {
  if (!selectedNodeId.value) return;
  pageNum.value = 1;
  await Promise.all([loadSummary(), loadRecords()]);
}

function onNodeChange(): void {
  if (!selectedNodeId.value) {
    summary.value = null;
    records.value = [];
    recordsTotal.value = 0;
    return;
  }
  void loadData();
}

function onPageChange(): void {
  void loadRecords();
}

function onPageSizeChange(): void {
  pageNum.value = 1;
  void loadRecords();
}

async function trigger(): Promise<void> {
  if (!selectedNodeId.value) return;
  triggering.value = true;
  try {
    const resp = await tlqConnectivityApi.triggerTest(selectedNodeId.value);
    const rttSuffix = resp.rttMs !== null && resp.rttMs !== undefined ? ` (${resp.rttMs}ms)` : '';
    const msg = `${resp.message}${rttSuffix}`;
    if (resp.result === 'SUCCESS') {
      ElMessage.success(msg);
    } else if (resp.result === 'TIMEOUT') {
      ElMessage.warning(msg);
    } else {
      ElMessage.error(msg);
    }
    await loadData();
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '触发测试失败';
    ElMessage.error(message);
  } finally {
    triggering.value = false;
  }
}

onMounted(loadNodes);

// Red line #1: narrow expose — selectedNodeId ref (jsdom cannot reliably
// render the el-select popover so tests mutate this ref before emitting
// the boundary 'change' event on ElSelect). summary / records exposed for
// read-only inspection in assertions. trigger / loadData / loadRecords /
// loadSummary intentionally NOT exposed — tests reach them through DOM
// button clicks and component-boundary emits.
defineExpose({ selectedNodeId, summary, records });
</script>

<style scoped>
.tlq-connectivity-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #eaeaea;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card .label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}
.stat-card .value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.stat-card .sub {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
.trigger-area {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}
</style>
