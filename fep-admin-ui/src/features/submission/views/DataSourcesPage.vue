<template>
  <div class="data-sources-page">
    <el-page-header
      title="数据报送管理"
      content="数据源管理"
      class="page-header"
    />

    <SearchForm
      @search="onSearch"
      @reset="onReset"
    >
      <el-form-item label="关键字">
        <el-input
          v-model="searchForm.keyword"
          placeholder="数据源名称"
          style="width: 200px"
          clearable
        />
      </el-form-item>
    </SearchForm>

    <el-button
      type="primary"
      class="create-btn"
      @click="openCreate"
    >
      + 新建数据源
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
      <template #pushEnabled="{ row }">
        <el-switch
          :model-value="row.pushEnabled"
          disabled
        />
      </template>
      <template #sourceStatus="{ row }">
        <StatusTag
          :value="row.sourceStatus"
          :mapping="ENABLE_DISABLE_STATUS_MAP"
        />
      </template>
      <template #operation>
        <el-table-column
          label="操作"
          min-width="160"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="onEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <DataSourceEditDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :record="editingRecord"
      @save="onSaveDialog"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  SearchForm,
  DataTable,
  StatusTag,
  type DataTableColumn,
} from '@/shared/components';
import { ENABLE_DISABLE_STATUS_MAP } from '@/shared/types/enum-maps';
import {
  subDataSourceApi,
  type DataSourceRequest,
  type DataSourceResponse,
  type DataSourceSearchParams,
} from '../api/sub-data-source-api';
import DataSourceEditDialog from '../components/DataSourceEditDialog.vue';
import type { PageResult } from '@/shared/types/page-result';

/**
 * FR-WEB-SUB-SRC Data Source management page (PRD §5.5.3).
 *
 * Contract notes baked into UI:
 *  - No toggleStatus API (AC #2 baseline).
 *  - Delete button triggers ElMessageBox.confirm, then API.remove and reload.
 *  - pushEnabled displayed as a read-only el-switch in the list.
 */
const searchForm = reactive<DataSourceSearchParams>({ pageNum: 1, pageSize: 10 });
const page = ref<PageResult<DataSourceResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
  totalPages: 0,
});
const loading = ref(false);

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const editingRecord = ref<DataSourceResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'sourceId', label: '数据源 ID', width: 120 },
  { prop: 'sourceName', label: '数据源名称', minWidth: 160 },
  { prop: 'contactAddress', label: '联系地址', minWidth: 200 },
  { prop: 'contactPhone', label: '联系电话', width: 140 },
  { prop: 'pushEnabled', label: '推送启用', width: 100, slot: 'pushEnabled' },
  { prop: 'contentType', label: '内容类型', width: 140 },
  { prop: 'clientId', label: '客户端 ID', width: 140 },
  { prop: 'sourceStatus', label: '状态', width: 90, slot: 'sourceStatus' },
  { prop: 'createTime', label: '创建时间', width: 170 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await subDataSourceApi.search(searchForm);
  } catch {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
}

function onSearch() { searchForm.pageNum = 1; refresh(); }
function onReset() {
  Object.assign(searchForm, { pageNum: 1, pageSize: 10, keyword: undefined });
  refresh();
}
function onPageNumChange(v: number) { searchForm.pageNum = v; refresh(); }
function onPageSizeChange(v: number) { searchForm.pageSize = v; searchForm.pageNum = 1; refresh(); }

function openCreate() {
  dialogMode.value = 'create';
  editingRecord.value = null;
  dialogVisible.value = true;
}

function onEdit(record: DataSourceResponse) {
  dialogMode.value = 'edit';
  editingRecord.value = record;
  dialogVisible.value = true;
}

async function onSaveDialog(payload: DataSourceRequest) {
  if (dialogMode.value === 'create') {
    await subDataSourceApi.create(payload);
    ElMessage.success('已创建');
  } else if (editingRecord.value) {
    await subDataSourceApi.update(editingRecord.value.sourceId, payload);
    ElMessage.success('已更新');
  }
  refresh();
}

async function onDelete(record: DataSourceResponse) {
  try {
    await ElMessageBox.confirm(
      `确认删除数据源 "${record.sourceName}" 吗？`,
      '确认',
      { type: 'warning' },
    );
  } catch {
    // User cancelled — do nothing.
    return;
  }
  // User confirmed — surface backend failures as a toast instead of an
  // uncaught rejection.
  try {
    await subDataSourceApi.remove(record.sourceId);
    ElMessage.success('已删除');
    await refresh();
  } catch {
    ElMessage.error('删除失败');
  }
}

defineExpose({ onDelete, openCreate, dialogVisible, dialogMode, editingRecord, page });

onMounted(refresh);
</script>

<style scoped>
.data-sources-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.create-btn { align-self: flex-start; }
</style>
