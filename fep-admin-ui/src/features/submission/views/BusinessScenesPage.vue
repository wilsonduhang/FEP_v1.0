<template>
  <div class="business-scenes-page">
    <el-page-header
      title="数据报送管理"
      content="业务场景管理"
      class="page-header"
    />

    <SearchForm
      @search="onSearch"
      @reset="onReset"
    >
      <el-form-item label="关键字">
        <el-input
          v-model="searchForm.keyword"
          placeholder="场景名称"
          style="width: 200px"
          clearable
        />
      </el-form-item>
      <el-form-item label="业务类型">
        <el-select
          v-model="searchForm.businessTypeId"
          placeholder="全部"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="opt in DEFAULT_BIZ_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
    </SearchForm>

    <el-button
      type="primary"
      class="create-btn"
      @click="openCreate"
    >
      + 新建业务场景
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
      <template #businessTypeId="{ row }">
        {{ renderBizType(row.businessTypeId) }}
      </template>
      <template #pushMethod="{ row }">
        <StatusTag
          :value="row.pushMethod"
          :mapping="SCENE_PUSH_METHOD_MAP"
        />
      </template>
      <template #sceneStatus="{ row }">
        <StatusTag
          :value="row.sceneStatus"
          :mapping="ENABLE_DISABLE_STATUS_MAP"
        />
      </template>
      <template #operation>
        <el-table-column
          label="操作"
          min-width="200"
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
              {{ row.sceneStatus === 'ENABLED' ? '停用' : '启用' }}
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

    <BusinessSceneEditDialog
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
import {
  ENABLE_DISABLE_STATUS_MAP,
  SCENE_PUSH_METHOD_MAP,
} from '@/shared/types/enum-maps';
import { DEFAULT_BIZ_TYPE_OPTIONS } from '@/features/biz-data/constants/biz-type-options';
import {
  subBusinessSceneApi,
  type BusinessSceneRequest,
  type BusinessSceneResponse,
  type BusinessSceneSearchParams,
} from '../api/sub-business-scene-api';
import BusinessSceneEditDialog from '../components/BusinessSceneEditDialog.vue';
import type { PageResult } from '@/shared/types/page-result';

/**
 * FR-WEB-SUB-SCENE Business Scene management page (PRD §5.5.4).
 *
 * Contract-baseline notes baked into UI:
 *  - `pushMethod` is 2-valued (AUTO | MANUAL); no SCHEDULE mode / no cron.
 *  - `toggleStatus` PATCH no body → update the single row from the response.
 *  - No upload endpoint; `importTemplatePath` is stored as a plain string path.
 */
const searchForm = reactive<BusinessSceneSearchParams>({ pageNum: 1, pageSize: 10 });
const page = ref<PageResult<BusinessSceneResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
  totalPages: 0,
});
const loading = ref(false);

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'edit'>('create');
const editingRecord = ref<BusinessSceneResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'sceneId', label: '场景 ID', width: 100 },
  { prop: 'sceneName', label: '场景名称', minWidth: 160 },
  { prop: 'businessTypeId', label: '业务类型', width: 160, slot: 'businessTypeId' },
  { prop: 'pushMethod', label: '推送方式', width: 100, slot: 'pushMethod' },
  { prop: 'requestUrl', label: '请求地址', minWidth: 240 },
  { prop: 'sortOrder', label: '排序', width: 80 },
  { prop: 'sceneStatus', label: '状态', width: 90, slot: 'sceneStatus' },
];

function renderBizType(id: string): string {
  const hit = DEFAULT_BIZ_TYPE_OPTIONS.find((o) => o.value === id);
  return hit ? hit.label : id;
}

async function refresh() {
  loading.value = true;
  try {
    page.value = await subBusinessSceneApi.search(searchForm);
  } catch {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
}

function onSearch() { searchForm.pageNum = 1; refresh(); }
function onReset() {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 10,
    keyword: undefined,
    businessTypeId: undefined,
  });
  refresh();
}
function onPageNumChange(v: number) { searchForm.pageNum = v; refresh(); }
function onPageSizeChange(v: number) { searchForm.pageSize = v; searchForm.pageNum = 1; refresh(); }

function openCreate() {
  dialogMode.value = 'create';
  editingRecord.value = null;
  dialogVisible.value = true;
}

function onEdit(record: BusinessSceneResponse) {
  dialogMode.value = 'edit';
  editingRecord.value = record;
  dialogVisible.value = true;
}

async function onSaveDialog(payload: BusinessSceneRequest) {
  if (dialogMode.value === 'create') {
    await subBusinessSceneApi.create(payload);
    ElMessage.success('已创建');
  } else if (editingRecord.value) {
    await subBusinessSceneApi.update(editingRecord.value.sceneId, payload);
    ElMessage.success('已更新');
  }
  refresh();
}

async function onToggleStatus(record: BusinessSceneResponse) {
  const action = record.sceneStatus === 'ENABLED' ? '停用' : '启用';
  await ElMessageBox.confirm(`确认${action}该业务场景？`, '提示', { type: 'warning' });
  const updated = await subBusinessSceneApi.toggleStatus(record.sceneId);
  // AC #2: update local row with returned record; do not re-fetch the list.
  const idx = page.value.records.findIndex((r) => r.sceneId === record.sceneId);
  if (idx >= 0) {
    page.value.records.splice(idx, 1, updated);
  }
  ElMessage.success(`已${action}`);
}

async function onDelete(record: BusinessSceneResponse) {
  try {
    await ElMessageBox.confirm(
      `确认删除业务场景 "${record.sceneName}" 吗？`,
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
    await subBusinessSceneApi.remove(record.sceneId);
    ElMessage.success('已删除');
    await refresh();
  } catch {
    ElMessage.error('删除失败');
  }
}

defineExpose({
  onToggleStatus,
  onDelete,
  openCreate,
  dialogVisible,
  dialogMode,
  editingRecord,
  page,
});

onMounted(refresh);
</script>

<style scoped>
.business-scenes-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.create-btn { align-self: flex-start; }
</style>
