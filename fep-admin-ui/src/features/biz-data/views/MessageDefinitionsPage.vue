<template>
  <div class="message-definitions-page">
    <el-page-header title="业务数据管理" content="报文定义" class="page-header" />

    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="关键字">
        <el-input v-model="searchForm.keyword" placeholder="报文编码或名称" style="width: 200px" />
      </el-form-item>
      <el-form-item label="报文编码">
        <el-input v-model="searchForm.messageCode" placeholder="精确匹配" style="width: 140px" />
      </el-form-item>
      <el-form-item label="方向">
        <el-select v-model="searchForm.direction" placeholder="全部" clearable style="width: 140px">
          <el-option label="出站" value="OUTBOUND" />
          <el-option label="入站" value="INBOUND" />
          <el-option label="双向" value="BIDIRECTIONAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="searchForm.definitionStatus"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </SearchForm>

    <el-button type="primary" class="create-btn" @click="openCreate"> + 新建报文定义 </el-button>

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
        <StatusTag :value="row.direction" :mapping="MESSAGE_DIRECTION_MAP" />
      </template>
      <template #definitionStatus="{ row }">
        <StatusTag :value="row.definitionStatus" :mapping="ENABLE_DISABLE_STATUS_MAP" />
      </template>
      <template #operation>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)"> 编辑 </el-button>
            <el-button link type="primary" @click="onToggleStatus(row)">
              {{ row.definitionStatus === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row.definitionId)"> 删除 </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <DefinitionEditDialog
      v-model="dialogVisible"
      :definition="editingDefinition"
      @saved="refresh"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SearchForm, DataTable, StatusTag } from '@/shared/components';
import type { DataTableColumn } from '@/shared/components';
import { MESSAGE_DIRECTION_MAP, ENABLE_DISABLE_STATUS_MAP } from '@/shared/types/enum-maps';
import {
  bizMessageDefinitionApi,
  type DefinitionResponse,
  type DefinitionSearchParams,
} from '../api/biz-message-definition-api';
import DefinitionEditDialog from '../components/DefinitionEditDialog.vue';
import type { PageResult } from '@/shared/types/page-result';

const searchForm = reactive<DefinitionSearchParams>({ pageNum: 1, pageSize: 20 });
const page = ref<PageResult<DefinitionResponse>>({
  records: [],
  total: 0,
  pageNum: 1,
  pageSize: 20,
  totalPages: 0,
});
const loading = ref(false);
const dialogVisible = ref(false);
const editingDefinition = ref<DefinitionResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'messageCode', label: '报文编码', width: 120 },
  { prop: 'messageName', label: '报文名称', minWidth: 200 },
  { prop: 'direction', label: '方向', width: 100, slot: 'direction' },
  { prop: 'fieldCount', label: '字段数', width: 80 },
  { prop: 'sortOrder', label: '排序', width: 80 },
  { prop: 'definitionStatus', label: '状态', width: 100, slot: 'definitionStatus' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await bizMessageDefinitionApi.search(searchForm);
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
    keyword: undefined,
    messageCode: undefined,
    direction: undefined,
    definitionStatus: undefined,
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

function openCreate() {
  editingDefinition.value = null;
  dialogVisible.value = true;
}

function onEdit(definition: DefinitionResponse) {
  editingDefinition.value = definition;
  dialogVisible.value = true;
}

async function onToggleStatus(definition: DefinitionResponse) {
  const action = definition.definitionStatus === 'ENABLED' ? '禁用' : '启用';
  await ElMessageBox.confirm(`确认${action}该报文定义？`, '提示', { type: 'warning' });
  await bizMessageDefinitionApi.toggleStatus(definition.definitionId);
  ElMessage.success(`已${action}`);
  refresh();
}

async function onDelete(definitionId: string) {
  await ElMessageBox.confirm('确认删除该报文定义？', '提示', { type: 'warning' });
  await bizMessageDefinitionApi.delete(definitionId);
  ElMessage.success('已删除');
  refresh();
}

onMounted(refresh);
</script>

<style scoped>
.message-definitions-page {
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
