<template>
  <div class="auth-letters-page">
    <el-page-header
      title="企业信息查询"
      content="授权书管理"
      class="page-header"
    >
      <template #extra>
        <el-tag type="warning">
          ⚠️ TLQ Mock 模式
        </el-tag>
      </template>
    </el-page-header>

    <SearchForm
      @search="onSearch"
      @reset="onReset"
    >
      <el-form-item label="状态">
        <el-select
          v-model="searchForm.letterStatus"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            label="草稿"
            value="DRAFT"
          />
          <el-option
            label="已提交"
            value="SUBMITTED"
          />
          <el-option
            label="已确认"
            value="ACKNOWLEDGED"
          />
          <el-option
            label="已拒绝"
            value="REJECTED"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="授权类型">
        <el-select
          v-model="searchForm.authType"
          placeholder="全部"
          clearable
          style="width: 140px"
        >
          <el-option
            label="纸质"
            value="PAPER"
          />
          <el-option
            label="电子"
            value="ELECTRONIC"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input
          v-model="searchForm.keyword"
          placeholder="USCI 或企业名"
          style="width: 200px"
        />
      </el-form-item>
    </SearchForm>

    <el-button
      type="primary"
      class="create-btn"
      @click="openCreate"
    >
      + 新建授权书
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
          :mapping="AUTH_TYPE_MAP"
        />
      </template>
      <template #letterStatus="{ row }">
        <StatusTag
          :value="row.letterStatus"
          :mapping="LETTER_STATUS_MAP"
        />
      </template>
      <template #operation>
        <el-table-column
          label="操作"
          min-width="260"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="onView(row)"
            >
              查看
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="row.letterStatus !== 'DRAFT'"
              @click="onEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="row.letterStatus !== 'DRAFT'"
              @click="onSubmit(row.letterId)"
            >
              提交
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.letterStatus !== 'DRAFT'"
              @click="onDelete(row.letterId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <AuthLetterEditDialog
      v-model="dialogVisible"
      :letter="editingLetter"
      @saved="refresh"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { SearchForm, DataTable, StatusTag } from '@/shared/components';
import type { DataTableColumn } from '@/shared/components';
import { AUTH_TYPE_MAP, LETTER_STATUS_MAP } from '@/shared/types/enum-maps';
import {
  entAuthLetterApi,
  type AuthLetterResponse,
  type AuthLetterSearchParams,
} from '../api/ent-auth-letter-api';
import AuthLetterEditDialog from '../components/AuthLetterEditDialog.vue';
import type { PageResult } from '@/shared/types/page-result';

const searchForm = reactive<AuthLetterSearchParams>({ pageNum: 1, pageSize: 20 });
const page = ref<PageResult<AuthLetterResponse>>({ records: [], total: 0, pageNum: 1, pageSize: 20, totalPages: 0 });
const loading = ref(false);
const dialogVisible = ref(false);
const editingLetter = ref<AuthLetterResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'letterId', label: '授权书 ID', minWidth: 160 },
  { prop: 'enterpriseId', label: '企业 ID', minWidth: 120 },
  { prop: 'authType', label: '类型', width: 90, slot: 'authType' },
  { prop: 'authorizedUsci', label: '被授权 USCI', minWidth: 180 },
  { prop: 'authorizedName', label: '被授权名称', minWidth: 180 },
  { prop: 'authScope', label: '授权范围', minWidth: 200 },
  { prop: 'letterStatus', label: '状态', width: 100, slot: 'letterStatus' },
  { prop: 'createTime', label: '创建时间', minWidth: 160 },
  { prop: 'updateTime', label: '更新时间', minWidth: 160 },
];

async function refresh() {
  loading.value = true;
  try {
    page.value = await entAuthLetterApi.search(searchForm);
  } finally {
    loading.value = false;
  }
}

function onSearch() { searchForm.pageNum = 1; refresh(); }
function onReset() {
  Object.assign(searchForm, { pageNum: 1, pageSize: 20, letterStatus: undefined, authType: undefined, keyword: undefined });
}
function onPageNumChange(v: number) { searchForm.pageNum = v; refresh(); }
function onPageSizeChange(v: number) { searchForm.pageSize = v; searchForm.pageNum = 1; refresh(); }

function openCreate() {
  editingLetter.value = null;
  dialogVisible.value = true;
}
function onView(letter: AuthLetterResponse) {
  editingLetter.value = letter;
  dialogVisible.value = true;
}
function onEdit(letter: AuthLetterResponse) {
  editingLetter.value = letter;
  dialogVisible.value = true;
}

async function onSubmit(letterId: string) {
  await ElMessageBox.confirm('确认提交该授权书？提交后不可编辑。', '提示', { type: 'warning' });
  await entAuthLetterApi.submit(letterId);
  ElMessage.success('已提交');
  refresh();
}

async function onDelete(letterId: string) {
  await ElMessageBox.confirm('确认删除该授权书？', '提示', { type: 'warning' });
  await entAuthLetterApi.delete(letterId);
  ElMessage.success('已删除');
  refresh();
}

onMounted(refresh);
</script>

<style scoped>
.auth-letters-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { padding-bottom: 12px; border-bottom: 1px solid #eaeaea; }
.create-btn { align-self: flex-start; }
</style>
