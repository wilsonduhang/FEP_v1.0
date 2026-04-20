<!-- src/features/tlq/views/TlqNodesPage.vue -->
<template>
  <div class="tlq-nodes-page">
    <el-page-header title="TLQ 节点管理" content="§5.7.1 节点配置列表" class="page-header" />

    <SearchForm @search="onSearch" @reset="onReset">
      <el-form-item label="角色">
        <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 220px">
          <el-option
            v-for="opt in ROLE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 200px">
          <el-option
            v-for="opt in STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
    </SearchForm>

    <div class="toolbar">
      <el-button type="primary" @click="openCreate"> 新建节点 </el-button>
    </div>

    <DataTable
      :data="page.records"
      :columns="columns"
      :loading="loading"
      :total="page.total"
      :page-num="pageNum"
      :page-size="pageSize"
      @update:page-num="onPageChange"
      @update:page-size="onPageSizeChange"
    >
      <template #nodeRole="{ row }">
        <StatusTag :value="row.nodeRole" :mapping="TLQ_NODE_ROLE_MAP" />
      </template>
      <template #vipAddress="{ row }">
        {{ row.vipAddress ?? '-' }}
      </template>
      <template #nodeStatus="{ row }">
        <StatusTag :value="row.nodeStatus" :mapping="TLQ_NODE_STATUS_MAP" />
      </template>
      <template #lastHeartbeat="{ row }">
        {{ row.lastHeartbeat ?? '-' }}
      </template>
      <template #operation>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)"> 编辑 </el-button>
            <el-button
              link
              type="primary"
              :disabled="!canTransitionTo(row.nodeStatus, 'ONLINE')"
              @click="transition(row, 'ONLINE')"
            >
              上线
            </el-button>
            <el-button
              link
              type="warning"
              :disabled="!canTransitionTo(row.nodeStatus, 'OFFLINE')"
              @click="transition(row, 'OFFLINE')"
            >
              下线
            </el-button>
            <el-button link type="danger" @click="confirmDelete(row)"> 删除 </el-button>
          </template>
        </el-table-column>
      </template>
    </DataTable>

    <!-- Task 3 replaces the placeholder with the real create/update form. -->
    <EditDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :record="editingRecord"
      @success="reload"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { DataTable, SearchForm, StatusTag } from '@/shared/components';
import type { DataTableColumn } from '@/shared/components';
import { TLQ_NODE_ROLE_MAP, TLQ_NODE_STATUS_MAP } from '@/shared/types/enum-maps';
import { tlqNodeApi, type NodeListParams } from '../api/tlq-node-api';
import type { TlqNodeResponse, TlqNodeRole, TlqNodeStatus } from '../types';
import EditDialog from '../components/TlqNodeEditDialog.vue';

/**
 * TLQ nodes page — §5.7.1 (FR-WEB-TLQ-CFG).
 *
 * <p>Lists TLQ node configurations with role/status filter, paginated table
 * with inline status transition buttons (state-machine gated), edit and
 * delete actions, and a create/update dialog provided by Task 3.</p>
 *
 * <p>Red line #1 compliance: user-observable actions are triggered through
 * native DOM events or child-component emitted events; internal refs are
 * exposed for filter mutation (verified via test red line #1 style).</p>
 */

interface Filters {
  role?: TlqNodeRole;
  status?: TlqNodeStatus;
}

const filters = reactive<Filters>({});
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const page = ref<{
  records: TlqNodeResponse[];
  total: number;
  pageNum: number;
  pageSize: number;
  totalPages: number;
}>({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });

const dialogVisible = ref(false);
const dialogMode = ref<'create' | 'update'>('create');
const editingRecord = ref<TlqNodeResponse | null>(null);

const columns: DataTableColumn[] = [
  { prop: 'nodeName', label: '节点名称', minWidth: 160 },
  { prop: 'nodeRole', label: '角色', width: 200, slot: 'nodeRole' },
  { prop: 'hostIp', label: 'IP', width: 150 },
  { prop: 'port', label: '端口', width: 100 },
  { prop: 'vipAddress', label: 'VIP', width: 160, slot: 'vipAddress' },
  { prop: 'protocol', label: '协议', width: 100 },
  { prop: 'nodeStatus', label: '状态', width: 110, slot: 'nodeStatus' },
  { prop: 'lastHeartbeat', label: '最近心跳', width: 180, slot: 'lastHeartbeat' },
];

const ROLE_OPTIONS = Object.entries(TLQ_NODE_ROLE_MAP).map(([value, { label }]) => ({
  value,
  label,
}));
const STATUS_OPTIONS = Object.entries(TLQ_NODE_STATUS_MAP).map(([value, { label }]) => ({
  value,
  label,
}));

// {@code TlqNodeEditDialog.vue} is a placeholder in Task 2; Task 3 replaces
// it with the real create/update form. Synchronous import keeps test mounts
// deterministic (defineAsyncComponent is not awaited before DataTable renders
// in jsdom, which caused row cells to stay empty).

async function reload(): Promise<void> {
  loading.value = true;
  const params: NodeListParams = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };
  if (filters.role !== undefined) {
    params.role = filters.role;
  }
  if (filters.status !== undefined) {
    params.status = filters.status;
  }
  try {
    page.value = await tlqNodeApi.listNodes(params);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载失败';
    ElMessage.error(msg);
  } finally {
    loading.value = false;
  }
}

function onSearch(): void {
  pageNum.value = 1;
  void reload();
}

function onReset(): void {
  filters.role = undefined;
  filters.status = undefined;
  pageNum.value = 1;
  void reload();
}

function onPageChange(next: number): void {
  pageNum.value = next;
  void reload();
}

function onPageSizeChange(size: number): void {
  pageSize.value = size;
  pageNum.value = 1;
  void reload();
}

function canTransitionTo(current: TlqNodeStatus, target: TlqNodeStatus): boolean {
  if (current === 'UNKNOWN' && target === 'ONLINE') return true;
  if (current === 'ONLINE' && target === 'OFFLINE') return true;
  if (current === 'OFFLINE' && target === 'ONLINE') return true;
  return false;
}

async function transition(row: TlqNodeResponse, target: TlqNodeStatus): Promise<void> {
  try {
    await tlqNodeApi.changeStatus(row.nodeId, target);
    ElMessage.success('状态已更新');
    await reload();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '状态更新失败';
    ElMessage.error(msg);
  }
}

async function confirmDelete(row: TlqNodeResponse): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除节点 ${row.nodeName}?`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
  } catch {
    // User dismissed the dialog — not an error.
    return;
  }
  try {
    await tlqNodeApi.deleteNode(row.nodeId);
    ElMessage.success('删除成功');
    await reload();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '存在关联队列，无法删除';
    ElMessage.error(msg);
  }
}

function openCreate(): void {
  dialogMode.value = 'create';
  editingRecord.value = null;
  dialogVisible.value = true;
}

function openEdit(row: TlqNodeResponse): void {
  dialogMode.value = 'update';
  editingRecord.value = row;
  dialogVisible.value = true;
}

onMounted(reload);

// Narrow expose surface: filters & pagination state are the only cross-component
// handoffs test interacts with via SearchForm event emission.
defineExpose({ filters, pageNum, pageSize, page });
</script>

<style scoped>
.tlq-nodes-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #eaeaea;
}
.toolbar {
  display: flex;
  justify-content: flex-start;
}
</style>
