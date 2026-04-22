<template>
  <div class="query-tasks-page">
    <el-page-header title="企业信息查询" content="查询任务管理" class="page-header">
      <template #extra>
        <el-tag type="warning"> ⚠️ TLQ Mock 模式 </el-tag>
      </template>
    </el-page-header>

    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="查询类型">
        <el-select v-model="searchForm.queryType" placeholder="全部" clearable style="width: 140px">
          <el-option label="实时" value="REALTIME" />
          <el-option label="批量" value="BATCH" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态">
        <el-select
          v-model="searchForm.taskStatus"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option label="草稿" value="DRAFT" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="searchForm.keyword" placeholder="USCI 或企业名" style="width: 200px" />
      </el-form-item>
    </SearchForm>

    <el-button type="primary" class="create-btn" @click="dialogVisible = true">
      + 新建查询任务
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
      <template #queryType="{ row }">
        <StatusTag :value="row.queryType" :mapping="QUERY_TYPE_MAP" />
      </template>
      <template #taskStatus="{ row }">
        <StatusTag :value="row.taskStatus" :mapping="QUERY_TASK_STATUS_MAP" />
      </template>
      <template #operation>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onShowDetail(row.taskId)"> 详情 </el-button>
            <el-button
              link
              type="primary"
              :disabled="row.taskStatus !== 'DRAFT'"
              @click="onExecute(row.taskId)"
            >
              执行
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.taskStatus !== 'DRAFT'"
              @click="onDelete(row.taskId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <QueryTaskCreateDialog v-model="dialogVisible" @created="refresh" />
    <QueryTaskDetailDrawer v-model="drawerVisible" :task-id="selectedTaskId" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SearchForm, DataTable, StatusTag } from '@/shared/components';
import type { DataTableColumn } from '@/shared/components';
import { QUERY_TYPE_MAP, QUERY_TASK_STATUS_MAP } from '@/shared/types/enum-maps';
import {
  entQueryTaskApi,
  type QueryTaskResponse,
  type QueryTaskSearchParams,
} from '../api/ent-query-task-api';
import QueryTaskCreateDialog from '../components/QueryTaskCreateDialog.vue';
import QueryTaskDetailDrawer from '../components/QueryTaskDetailDrawer.vue';
import type { PageResult } from '@/shared/types/page-result';

const searchForm = reactive<QueryTaskSearchParams>({ pageNum: 1, pageSize: 20 });
const page = ref<PageResult<QueryTaskResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 20,
  totalPages: 0,
});
const loading = ref(false);
const dialogVisible = ref(false);
const drawerVisible = ref(false);
const selectedTaskId = ref<string>('');

const columns: DataTableColumn[] = [
  { prop: 'taskId', label: '任务 ID', minWidth: 160 },
  { prop: 'enterpriseId', label: '企业 ID', minWidth: 120 },
  { prop: 'queryType', label: '查询类型', width: 100, slot: 'queryType' },
  { prop: 'usci', label: 'USCI', minWidth: 180 },
  { prop: 'queryTargetName', label: '被查询企业', minWidth: 180 },
  { prop: 'taskStatus', label: '状态', width: 100, slot: 'taskStatus' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'completeTime', label: '完成时间', minWidth: 160 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await entQueryTaskApi.search(searchForm);
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  searchForm.pageNum = 1;
  refresh();
}
function onReset() {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 20,
    queryType: undefined,
    taskStatus: undefined,
    keyword: undefined,
  });
}
function onPageNumChange(v: number) {
  searchForm.pageNum = v;
  refresh();
}
function onPageSizeChange(v: number) {
  searchForm.pageSize = v;
  searchForm.pageNum = 1;
  refresh();
}

async function onExecute(taskId: string) {
  await entQueryTaskApi.execute(taskId);
  ElMessage.success('已触发执行（Mock Mode 下不会真实发往 TLQ）');
  refresh();
}
async function onDelete(taskId: string) {
  await ElMessageBox.confirm('确认删除该任务？', '提示', { type: 'warning' });
  await entQueryTaskApi.delete(taskId);
  ElMessage.success('已删除');
  refresh();
}
function onShowDetail(taskId: string) {
  selectedTaskId.value = taskId;
  drawerVisible.value = true;
}

onMounted(refresh);
</script>

<style scoped>
.query-tasks-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #eaeaea;
}
.create-btn {
  align-self: flex-start;
}
</style>
