<template>
  <div class="output-interfaces-page">
    <el-page-header
      title="数据报送管理"
      content="输出接口管理"
      class="page-header"
    />

    <SearchForm
      @search="onSearch"
      @reset="onReset"
    >
      <el-form-item label="关键字">
        <el-input
          v-model="searchForm.keyword"
          placeholder="接口名称"
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
      + 新建输出接口
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
      <template #authType="{ row }">
        <StatusTag
          :value="row.authType"
          :mapping="INTERFACE_AUTH_TYPE_MAP"
        />
      </template>
      <template #interfaceStatus="{ row }">
        <StatusTag
          :value="row.interfaceStatus"
          :mapping="ENABLE_DISABLE_STATUS_MAP"
        />
      </template>
      <template #operation>
        <el-table-column
          label="操作"
          min-width="280"
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
              type="primary"
              @click="onToggleStatus(row)"
            >
              {{ row.interfaceStatus === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="primary"
              @click="onTest(row.interfaceId)"
            >
              连通性测试
            </el-button>
            <MockBadge size="small">
              当前 Mock
            </MockBadge>
            <el-button
              link
              type="danger"
              @click="onDelete(row.interfaceId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <OutputInterfaceEditDialog
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
  MockBadge,
  type DataTableColumn,
} from '@/shared/components';
import {
  ENABLE_DISABLE_STATUS_MAP,
  INTERFACE_AUTH_TYPE_MAP,
} from '@/shared/types/enum-maps';
import {
  subOutputInterfaceApi,
  type OutputInterfaceRequest,
  type OutputInterfaceResponse,
  type OutputInterfaceSearchParams,
} from '../api/sub-output-interface-api';
import OutputInterfaceEditDialog from '../components/OutputInterfaceEditDialog.vue';
import type { PageResult } from '@/shared/types/page-result';

/**
 * FR-WEB-SUB-OUT Output Interface management page (PRD §5.5.2).
 *
 * Contract notes baked into UI:
 *  - toggleStatus PATCH no body → update local row with the returned record.
 *  - test POST returns bare Boolean → success/error message via ElMessage.
 *  - MockBadge always renders next to the connectivity-test button until P1
 *    real TLQ/HTTP probe replaces the current mock.
 */
const searchForm = reactive<OutputInterfaceSearchParams>({ pageNum: 1, pageSize: 10 });
const page = ref<PageResult<OutputInterfaceResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
  totalPages: 0,
});
const loading = ref(false);

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const editingRecord = ref<OutputInterfaceResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'interfaceId', label: '接口 ID', width: 120 },
  { prop: 'interfaceName', label: '接口名称', minWidth: 160 },
  { prop: 'interfaceUrl', label: '接口地址', minWidth: 240 },
  { prop: 'authType', label: '鉴权', width: 100, slot: 'authType' },
  { prop: 'timeoutSeconds', label: '超时(s)', width: 90 },
  { prop: 'retryCount', label: '重试', width: 80 },
  { prop: 'interfaceStatus', label: '状态', width: 90, slot: 'interfaceStatus' },
  { prop: 'lastCallTime', label: '最近调用', width: 170 },
  { prop: 'callCount', label: '调用次数', width: 100 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await subOutputInterfaceApi.search(searchForm);
  } finally {
    loading.value = false;
  }
}

function onSearch() { searchForm.pageNum = 1; refresh(); }
function onReset() {
  Object.assign(searchForm, { pageNum: 1, pageSize: 10, keyword: undefined });
}
function onPageNumChange(v: number) { searchForm.pageNum = v; refresh(); }
function onPageSizeChange(v: number) { searchForm.pageSize = v; searchForm.pageNum = 1; refresh(); }

function openCreate() {
  dialogMode.value = 'create';
  editingRecord.value = null;
  dialogVisible.value = true;
}

function onEdit(record: OutputInterfaceResponse) {
  dialogMode.value = 'edit';
  editingRecord.value = record;
  dialogVisible.value = true;
}

async function onSaveDialog(payload: OutputInterfaceRequest) {
  if (dialogMode.value === 'create') {
    await subOutputInterfaceApi.create(payload);
    ElMessage.success('已创建');
  } else if (editingRecord.value) {
    await subOutputInterfaceApi.update(editingRecord.value.interfaceId, payload);
    ElMessage.success('已更新');
  }
  refresh();
}

async function onToggleStatus(record: OutputInterfaceResponse) {
  const action = record.interfaceStatus === 'ENABLED' ? '停用' : '启用';
  await ElMessageBox.confirm(`确认${action}该输出接口？`, '提示', { type: 'warning' });
  const updated = await subOutputInterfaceApi.toggleStatus(record.interfaceId);
  // AC #2: update local row with returned record; do not re-fetch the list.
  const idx = page.value.records.findIndex((r) => r.interfaceId === record.interfaceId);
  if (idx >= 0) {
    page.value.records.splice(idx, 1, updated);
  }
  ElMessage.success(`已${action}`);
}

async function onTest(interfaceId: string) {
  const ok = await subOutputInterfaceApi.test(interfaceId);
  if (ok) {
    ElMessage.success('测试通过');
  } else {
    ElMessage.error('测试失败');
  }
}

async function onDelete(interfaceId: string) {
  await ElMessageBox.confirm('确认删除该输出接口？', '提示', { type: 'warning' });
  await subOutputInterfaceApi.remove(interfaceId);
  ElMessage.success('已删除');
  refresh();
}

defineExpose({ onTest, onToggleStatus, page });

onMounted(refresh);
</script>

<style scoped>
.output-interfaces-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.create-btn { align-self: flex-start; }
</style>
