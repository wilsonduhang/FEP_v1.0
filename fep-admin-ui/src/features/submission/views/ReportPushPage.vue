<!-- src/features/submission/views/ReportPushPage.vue -->
<template>
  <div class="report-push-page">
    <div class="header">
      <el-page-header title="报送管理" content="报文推送" class="page-header" />
    </div>

    <el-card class="relation-card" shadow="never">
      <div class="relation-row">
        <div>
          <div class="relation-title">推送关联</div>
          <div class="relation-desc">推送关联由输出接口管理统一维护。本页仅负责手动触发已配置的报送记录进入推送队列。</div>
        </div>
        <div>
          <MockBadge size="small" style="margin-right: 12px">真实 TLQ 推送 P1 就绪后启用</MockBadge>
          <el-button @click="$router.push('/submit/output-interfaces')">前往输出接口管理</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>待推送记录（PENDING）</span>
          <el-button
            type="primary"
            :disabled="selected.length === 0"
            :loading="pushing"
            data-test="trigger-push"
            @click="onTriggerPush"
          >
            触发推送（选中 {{ selected.length }} 条）
          </el-button>
        </div>
      </template>

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        data-test="pending-limit-alert"
        style="margin-bottom: 12px"
      >
        最近 500 条记录中的 PENDING 子集；完整 PENDING 需后端加 `pushStatus` 过滤参数（ticket #11）。推送触发后请手动刷新。
      </el-alert>

      <el-table
        :data="pendingRecords"
        v-loading="loadingPending"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="recordId" label="记录 ID" min-width="180" />
        <el-table-column prop="messageType" label="报文类型" width="100" />
        <el-table-column prop="messageName" label="报文名称" min-width="200" />
        <el-table-column label="金额(万元)" width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="dataCount" label="数据数" width="100" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <BlockedRecordsPanel ref="blockedPanel" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { MockBadge } from '@/shared/components';
import { formatAmount, formatDateTime } from '@/shared/utils/format';
import { subReportApi, type SubmissionRecordResponse } from '../api/sub-report-api';
import BlockedRecordsPanel from '../components/BlockedRecordsPanel.vue';

const pendingRecords = ref<SubmissionRecordResponse[]>([]);
const selected = ref<SubmissionRecordResponse[]>([]);
const loadingPending = ref(false);
const pushing = ref(false);
const blockedPanel = ref<InstanceType<typeof BlockedRecordsPanel>>();

async function loadPending(): Promise<void> {
  loadingPending.value = true;
  try {
    const resp = await subReportApi.searchRecords({ pageNum: 1, pageSize: 500 });
    pendingRecords.value = resp.records.filter((r) => r.pushStatus === 'PENDING');
  } catch (err: any) {
    ElMessage.error(err?.message ?? '待推送列表加载失败');
  } finally {
    loadingPending.value = false;
  }
}

function onSelectionChange(rows: SubmissionRecordResponse[]): void {
  selected.value = rows;
}

async function onTriggerPush(): Promise<void> {
  if (selected.value.length === 0) return;
  pushing.value = true;
  try {
    const ids = selected.value.map((r) => r.recordId);
    await subReportApi.triggerPush(ids);
    ElMessage.success(`已触发 ${ids.length} 条推送`);
    selected.value = [];
    await Promise.all([loadPending(), blockedPanel.value?.refresh()]);
  } catch (err: any) {
    if (err?.code === 'BIZ_5003') {
      ElMessage.warning('没有待推送的记录');
    } else {
      ElMessage.error(err?.message ?? '触发推送失败');
    }
  } finally {
    pushing.value = false;
  }
}

onMounted(loadPending);
defineExpose({ pendingRecords, selected, onTriggerPush, loadPending });
</script>

<style scoped>
.report-push-page { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.page-header { flex: 1; padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.relation-row { display: flex; justify-content: space-between; align-items: center; }
.relation-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.relation-desc { color: #606266; font-size: 13px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
</style>
