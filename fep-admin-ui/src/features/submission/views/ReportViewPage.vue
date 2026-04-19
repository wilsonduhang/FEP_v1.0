<!-- src/features/submission/views/ReportViewPage.vue -->
<template>
  <div class="report-view-page">
    <div class="header">
      <el-page-header title="报送管理" :content="title" class="page-header" />
      <MockBadge size="small">导出功能 P1 就绪后启用</MockBadge>
    </div>

    <div v-if="!messageType" class="guide">
      <el-empty description="请从报文数据列表或报送信息列表点击 '查看' 进入本页面">
        <el-button type="primary" @click="$router.push('/submit/message-summary')">
          前往报文数据列表
        </el-button>
      </el-empty>
    </div>

    <template v-else>
      <ViewStatsCards :total-count="totalCount" :pushed-count="pushedCount" />
      <ViewFilterBar :data-type-options="[]" @apply="onFilterApply" />

      <el-alert
        v-if="isFiltering"
        type="warning"
        :closable="false"
        show-icon
        data-test="filter-active-alert"
      >
        已筛选：当前页匹配 {{ filteredRecords.length }} 条（完整跨页筛选需后端支持，见 ticket
        #3/#4；分页控件在筛选激活时已禁用）
      </el-alert>

      <ViewTrendChart :points="trend" />

      <el-table v-loading="loading" :data="filteredRecords" stripe border>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column prop="recordId" label="记录 ID" min-width="180" />
        <el-table-column prop="businessNo" label="业务编号" min-width="140" />
        <el-table-column label="金额(万元)" width="140" align="right">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="dataCount" label="数据数" width="100" />
        <el-table-column label="录入方式" width="110">
          <template #default="{ row }">
            <StatusTag :value="row.entryMethod" :mapping="SUB_ENTRY_METHOD_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="推送状态" width="110">
          <template #default="{ row }">
            <StatusTag :value="row.pushStatus" :mapping="PUSH_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="page.total"
        :page-sizes="[10, 20, 50, 100]"
        :disabled="isFiltering"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="loadAll"
        @size-change="(s: number) => { pageSize = s; pageNum = 1; loadAll(); }"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { MockBadge, StatusTag } from '@/shared/components';
import { SUB_ENTRY_METHOD_MAP, PUSH_STATUS_MAP } from '@/shared/types/enum-maps';
import { formatAmount, formatDateTime } from '@/shared/utils/format';
import type { PageResult } from '@/shared/types/page-result';
import {
  subReportApi,
  type SubmissionRecordResponse,
  type TrendPoint,
} from '../api/sub-report-api';
import { subMessageSummaryApi } from '../api/sub-message-summary-api';
import { bizMessageDefinitionApi } from '@/features/biz-data/api/biz-message-definition-api';
import ViewStatsCards from '../components/ViewStatsCards.vue';
import ViewTrendChart from '../components/ViewTrendChart.vue';
import ViewFilterBar, { type FilterState } from '../components/ViewFilterBar.vue';

/**
 * §5.6.3 — By-type data detail view (FR-WEB-REP-VIEW).
 *
 * <p>URL-param driven: {@code /report/view?messageType=3001} triggers a
 * 3-way parallel fetch via {@code Promise.allSettled}:</p>
 * <ol>
 *   <li>{@code subMessageSummaryApi.getSummary()} — message-type aggregates
 *       (total / pushed counts + name)</li>
 *   <li>{@code subReportApi.getByMessageType(type, pageNum, pageSize)} —
 *       paginated detail records</li>
 *   <li>{@code subReportApi.getTrend(type)} — monthly aggregation for the
 *       trend chart</li>
 * </ol>
 *
 * <p>Each call fails independently via {@link ElMessage.error}. Title degrades
 * gracefully when the summary does not contain the requested message type
 * (v1b P1-new-#2: fallback to {@code bizMessageDefinitionApi.search}).</p>
 *
 * <p>Filter applies to the current page only (v1b-final P1-C-#1): across 3
 * axes — dataType → businessTypeId, reportType → entryMethod, dateRange →
 * createTime. Pagination is disabled while the filter is active to avoid
 * cross-page confusion.</p>
 */
const route = useRoute();
const messageType = computed(() => (route.query.messageType as string) || '');
const messageName = ref('');
const totalCount = ref(0);
const pushedCount = ref(0);
const trend = ref<TrendPoint[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const page = ref<PageResult<SubmissionRecordResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
  totalPages: 0,
});
const currentFilter = ref<FilterState>({ dataType: '', reportType: '', dateRange: null });

const title = computed(() =>
  messageName.value ? `数据明细（${messageName.value} 明细文件）` : '查看报送数据',
);

const filteredRecords = computed(() => {
  const { dataType, reportType, dateRange } = currentFilter.value;
  return page.value.records.filter((r) => {
    if (dataType && r.businessTypeId !== dataType) return false;
    if (reportType && r.entryMethod !== reportType) return false;
    if (dateRange) {
      const t = new Date(r.createTime).getTime();
      if (t < new Date(dateRange[0]).getTime()) return false;
      if (t > new Date(dateRange[1]).getTime()) return false;
    }
    return true;
  });
});

const isFiltering = computed(
  () =>
    currentFilter.value.dataType !== '' ||
    currentFilter.value.reportType !== '' ||
    currentFilter.value.dateRange !== null,
);

async function loadAll(): Promise<void> {
  if (!messageType.value) return;
  loading.value = true;
  const type = messageType.value;
  const results = await Promise.allSettled([
    subMessageSummaryApi.getSummary(),
    subReportApi.getByMessageType(type, pageNum.value, pageSize.value),
    subReportApi.getTrend(type),
  ]);

  if (results[0].status === 'fulfilled') {
    const item = results[0].value.find((s) => s.messageType === type);
    if (item) {
      messageName.value = item.messageName;
      totalCount.value = item.totalCount;
      pushedCount.value = item.pushedCount;
    } else {
      try {
        const defPage = await bizMessageDefinitionApi.search({
          pageNum: 1,
          pageSize: 1,
          messageCode: type,
        });
        if (defPage.records.length > 0) {
          messageName.value = defPage.records[0].messageName;
        }
      } catch {
        // Silent — title degrades to '查看报送数据'
      }
      totalCount.value = 0;
      pushedCount.value = 0;
    }
  } else {
    ElMessage.error('统计卡片加载失败');
  }

  if (results[1].status === 'fulfilled') {
    page.value = results[1].value;
  } else {
    ElMessage.error('明细列表加载失败');
  }

  if (results[2].status === 'fulfilled') {
    trend.value = results[2].value;
  } else {
    ElMessage.error('趋势数据加载失败');
  }

  loading.value = false;
}

function onFilterApply(v: FilterState): void {
  currentFilter.value = v;
}

watch(
  messageType,
  () => {
    pageNum.value = 1;
    loadAll();
  },
  { immediate: true },
);

defineExpose({
  loadAll,
  onFilterApply,
  messageType,
  messageName,
  totalCount,
  pushedCount,
  trend,
  page,
  filteredRecords,
  currentFilter,
  isFiltering,
});
</script>

<style scoped>
.report-view-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-header {
  flex: 1;
  padding-bottom: 12px;
  border-bottom: 1px solid #eaeaea;
}
.guide {
  margin-top: 60px;
  text-align: center;
}
.pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
