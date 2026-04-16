<template>
  <div class="message-records-page">
    <el-page-header
      title="业务数据管理"
      content="报文记录"
      class="page-header"
    >
      <template #extra>
        <el-tag type="warning">
          ⚠️ TLQ Mock 模式 (resubmit/export 为预留操作)
        </el-tag>
      </template>
    </el-page-header>

    <RecordSummaryCards :items="summaryItems" />

    <SearchForm
      @search="onSearch"
      @reset="onReset"
    >
      <el-form-item label="报文编码">
        <el-input
          v-model="searchForm.messageCode"
          placeholder="报文编码"
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="searchForm.status"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            label="待处理"
            value="PENDING"
          />
          <el-option
            label="处理中"
            value="PROCESSING"
          />
          <el-option
            label="成功"
            value="SUCCESS"
          />
          <el-option
            label="失败"
            value="FAILED"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="方向">
        <el-select
          v-model="searchForm.direction"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            label="出站"
            value="OUTBOUND"
          />
          <el-option
            label="入站"
            value="INBOUND"
          />
          <el-option
            label="双向"
            value="BIDIRECTIONAL"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker
          v-model="searchForm.startDate"
          type="date"
          placeholder="开始日期"
          value-format="YYYY-MM-DD"
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker
          v-model="searchForm.endDate"
          type="date"
          placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 160px"
        />
      </el-form-item>
    </SearchForm>

    <el-button
      type="primary"
      :loading="exporting"
      class="export-btn"
      @click="onExport"
    >
      导出
    </el-button>

    <DataTable
      :data="page.records"
      :columns="columns"
      :loading="loading"
      :total="page.total"
      :page-num="searchForm.pageNum"
      :page-size="searchForm.pageSize"
      @update:page-num="onPageNumChange"
      @update:page-size="onPageSizeChange"
    >
      <template #direction="{ row }">
        <StatusTag
          :value="row.direction"
          :mapping="MESSAGE_DIRECTION_MAP"
        />
      </template>
      <template #processStatus="{ row }">
        <StatusTag
          :value="row.processStatus"
          :mapping="MESSAGE_PROCESS_STATUS_MAP"
        />
      </template>
      <template #entryMethod="{ row }">
        <StatusTag
          :value="row.entryMethod"
          :mapping="ENTRY_METHOD_MAP"
        />
      </template>
      <template #amount="{ row }">
        <span v-if="row.amount">
          {{ formatCNY(row.amount) }}
        </span>
        <span v-else>-</span>
      </template>
      <template #operation>
        <el-table-column
          label="操作"
          min-width="120"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="onShowDetail(row.recordId)"
            >
              详情
            </el-button>
            <el-button
              link
              type="warning"
              :disabled="row.processStatus !== 'FAILED'"
              @click="onResubmit(row.recordId)"
            >
              重提
            </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <RecordDetailDrawer
      v-model="drawerVisible"
      :record-id="selectedRecordId"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SearchForm, DataTable, StatusTag } from '@/shared/components';
import type { DataTableColumn } from '@/shared/components';
import {
  MESSAGE_DIRECTION_MAP,
  MESSAGE_PROCESS_STATUS_MAP,
  ENTRY_METHOD_MAP,
} from '@/shared/types/enum-maps';
import {
  bizMessageRecordApi,
  type RecordResponse,
  type RecordSummaryItem,
  type RecordSearchParams,
} from '../api/biz-message-record-api';
import RecordSummaryCards from '../components/RecordSummaryCards.vue';
import RecordDetailDrawer from '../components/RecordDetailDrawer.vue';
import { useRecordExport } from '../composables/useRecordExport';
import type { PageResult } from '@/shared/types/page-result';

const cnyCurrencyFormat = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
});

function formatCNY(value: string): string {
  return cnyCurrencyFormat.format(parseFloat(value));
}

const searchForm = reactive<RecordSearchParams>({ pageNum: 1, pageSize: 20 });
const page = ref<PageResult<RecordResponse>>({ records: [], total: 0, pageNum: 1, pageSize: 20, totalPages: 0 });
const summaryItems = ref<RecordSummaryItem[]>([]);
const loading = ref(false);
const drawerVisible = ref(false);
const selectedRecordId = ref<string>('');
const { exporting, triggerExport } = useRecordExport();

const columns: DataTableColumn[] = [
  { prop: 'recordId', label: '记录 ID', minWidth: 160 },
  { prop: 'messageCode', label: '报文编码', width: 120 },
  { prop: 'serialNo', label: '流水号', minWidth: 140 },
  { prop: 'direction', label: '方向', width: 90, slot: 'direction' },
  { prop: 'processStatus', label: '状态', width: 100, slot: 'processStatus' },
  { prop: 'entryMethod', label: '录入', width: 80, slot: 'entryMethod' },
  { prop: 'businessNo', label: '业务编号', minWidth: 140 },
  { prop: 'amount', label: '金额', minWidth: 120, slot: 'amount' },
  { prop: 'accessCount', label: '访问次数', width: 90 },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await bizMessageRecordApi.search(searchForm);
  } finally {
    loading.value = false;
  }
}

async function loadSummary() {
  summaryItems.value = await bizMessageRecordApi.getSummary();
}

function onSearch() { searchForm.pageNum = 1; refresh(); }
function onReset() {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 20,
    messageCode: undefined,
    status: undefined,
    direction: undefined,
    startDate: undefined,
    endDate: undefined,
  });
}
function onPageNumChange(v: number) { searchForm.pageNum = v; refresh(); }
function onPageSizeChange(v: number) { searchForm.pageSize = v; searchForm.pageNum = 1; refresh(); }

function onShowDetail(recordId: string) {
  selectedRecordId.value = recordId;
  drawerVisible.value = true;
}

async function onResubmit(recordId: string) {
  await ElMessageBox.confirm('确认重新提交该报文？', '提示', { type: 'warning' });
  await bizMessageRecordApi.resubmit(recordId);
  ElMessage.success('已触发重提（Mock Mode）');
  refresh();
}

function onExport() {
  triggerExport(searchForm);
}

onMounted(() => {
  refresh();
  loadSummary();
});
</script>

<style scoped>
.message-records-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
</style>
