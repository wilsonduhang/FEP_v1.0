<!-- src/features/submission/components/BlockedRecordsPanel.vue -->
<template>
  <el-card shadow="never">
    <template #header>
      <div class="section-header">
        <span>不可推送记录（FAILED）</span>
        <el-button link type="primary" @click="refresh">刷新</el-button>
      </div>
    </template>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
      data-test="blocked-semantics-alert"
    >
      仅展示推送失败记录（FAILED）；PUSHING 属正常流转，请到报送信息列表查看
    </el-alert>
    <el-table :data="failedRecords" v-loading="loading">
      <el-table-column prop="recordId" label="记录 ID" min-width="180" />
      <el-table-column prop="messageType" label="报文类型" width="100" />
      <el-table-column prop="messageName" label="报文名称" min-width="180" />
      <el-table-column label="推送状态" width="110">
        <template #default="{ row }">
          <StatusTag :value="row.pushStatus" :mapping="PUSH_STATUS_MAP" />
        </template>
      </el-table-column>
      <el-table-column label="推送时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.pushTime) }}</template>
      </el-table-column>
      <el-table-column label="错误原因" min-width="200">
        <template #default="{ row }">
          <span v-if="row.errorMessage" class="error-msg">{{ row.errorMessage }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="page.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      @current-change="refresh"
      @size-change="refresh"
      class="pagination"
    />
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { StatusTag } from '@/shared/components';
import { PUSH_STATUS_MAP } from '@/shared/types/enum-maps';
import { formatDateTime } from '@/shared/utils/format';
import type { PageResult } from '@/shared/types/page-result';
import { subReportApi, type SubmissionRecordResponse } from '../api/sub-report-api';

const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const page = ref<PageResult<SubmissionRecordResponse>>({
  records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0,
});

const failedRecords = computed(() =>
  page.value.records.filter((r) => r.pushStatus === 'FAILED'),
);

async function refresh(): Promise<void> {
  loading.value = true;
  try {
    page.value = await subReportApi.getBlocked(pageNum.value, pageSize.value);
  } catch (err: any) {
    ElMessage.error(err?.message ?? '阻塞记录加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(refresh);
defineExpose({ refresh, page, failedRecords });
</script>

<style scoped>
.section-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.error-msg { color: #f56c6c; }
</style>
