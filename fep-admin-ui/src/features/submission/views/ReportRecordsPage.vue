<!-- src/features/submission/views/ReportRecordsPage.vue -->
<template>
  <div class="report-records-page">
    <el-page-header title="报送管理" content="报送信息列表" class="page-header" />

    <div class="stat-and-badge">
      <el-card class="stat-card" shadow="never">
        <div class="stat-value">{{ page.total }}</div>
        <div class="stat-label">总报文数</div>
      </el-card>
      <MockBadge size="small">批量操作 P1 后启用</MockBadge>
    </div>

    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="关键字">
        <el-input v-model="searchForm.keyword" placeholder="报文名称 / 业务编号" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="起始"
          end-placeholder="截止"
        />
      </el-form-item>
    </SearchForm>

    <div class="batch-bar">
      <el-button
        type="primary"
        :disabled="true"
        data-test="batch-action"
        title="批量操作 P1 后启用"
      >
        批量操作（{{ selectedRows.length }}）
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="page.records"
      stripe
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" fixed="left" />
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="submitterName" label="报送单位" min-width="140" />
      <el-table-column prop="businessNo" label="业务编号" min-width="140" />
      <el-table-column prop="messageName" label="报文名称" min-width="180" />
      <el-table-column label="金额(万元)" width="140" align="right">
        <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="dataCount" label="数据总数" width="100" />
      <el-table-column label="录入方式" width="100">
        <template #default="{ row }">
          <StatusTag :value="row.entryMethod" :mapping="SUB_ENTRY_METHOD_MAP" />
        </template>
      </el-table-column>
      <el-table-column prop="entryBy" label="录入人" width="120" />
      <el-table-column label="新增时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onView(row)">查看详情</el-button>
          <el-button link type="primary" @click="onJumpView(row)">查看明细</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="searchForm.pageNum"
      v-model:page-size="searchForm.pageSize"
      :total="page.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @current-change="refresh"
      @size-change="(s: number) => { searchForm.pageSize = s; searchForm.pageNum = 1; refresh(); }"
    />

    <RecordDetailDrawer v-model="drawerVisible" :record="drawerRecord" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { SearchForm, StatusTag, MockBadge } from '@/shared/components';
import { SUB_ENTRY_METHOD_MAP } from '@/shared/types/enum-maps';
import { formatAmount, formatDateTime } from '@/shared/utils/format';
import type { PageResult } from '@/shared/types/page-result';
import {
  subReportApi,
  type SubmissionRecordResponse,
  type ReportSearchParams,
} from '../api/sub-report-api';
import RecordDetailDrawer from '../components/RecordDetailDrawer.vue';

const router = useRouter();
const searchForm = reactive<ReportSearchParams>({ pageNum: 1, pageSize: 10 });
const dateRange = ref<[string, string] | null>(null);
const loading = ref(false);
const page = ref<PageResult<SubmissionRecordResponse>>({
  records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0,
});
const selectedRows = ref<SubmissionRecordResponse[]>([]);
const drawerVisible = ref(false);
const drawerRecord = ref<SubmissionRecordResponse | null>(null);

watch(dateRange, (v) => {
  searchForm.startTime = v?.[0];
  searchForm.endTime = v?.[1];
});

function onSelectionChange(rows: SubmissionRecordResponse[]): void {
  selectedRows.value = rows;
}

async function refresh(): Promise<void> {
  loading.value = true;
  try {
    page.value = await subReportApi.searchRecords(searchForm);
  } catch (err: any) {
    ElMessage.error(err?.message ?? '加载失败');
  } finally {
    loading.value = false;
  }
}

function onSearch(): void { searchForm.pageNum = 1; refresh(); }
function onReset(): void {
  Object.assign(searchForm, { pageNum: 1, pageSize: 10, keyword: undefined, startTime: undefined, endTime: undefined });
  dateRange.value = null;
  refresh();
}

function onView(row: SubmissionRecordResponse): void {
  drawerRecord.value = row;
  drawerVisible.value = true;
}
function onJumpView(row: { messageType: string }): void {
  router.push({ path: '/report/view', query: { messageType: row.messageType } });
}

onMounted(refresh);
defineExpose({ page, drawerVisible, drawerRecord, selectedRows, onView, onJumpView, onSelectionChange });
</script>

<style scoped>
.report-records-page { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.stat-and-badge { display: flex; align-items: center; gap: 12px; }
.stat-card { width: 220px; }
.stat-value { font-size: 32px; font-weight: 600; color: #409eff; }
.stat-label { color: #909399; font-size: 14px; margin-top: 4px; }
.batch-bar { display: flex; justify-content: flex-start; }
.pagination { display: flex; justify-content: flex-end; }
</style>
