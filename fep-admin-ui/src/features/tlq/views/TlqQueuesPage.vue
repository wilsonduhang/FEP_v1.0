<!-- src/features/tlq/views/TlqQueuesPage.vue -->
<template>
  <div class="tlq-queues-page">
    <el-page-header
      title="TLQ 队列配置"
      content="§5.7.1 队列列表"
      class="page-header"
    />

    <div class="toolbar">
      <el-select
        v-model="selectedNodeId"
        placeholder="请选择节点"
        style="width: 340px"
        filterable
        clearable
        @change="onNodeChange"
      >
        <el-option
          v-for="n in nodes"
          :key="n.nodeId"
          :value="n.nodeId"
          :label="`${n.nodeName} (${n.hostIp}:${n.port})`"
        />
      </el-select>
      <el-button
        type="primary"
        :disabled="!selectedNodeId"
        @click="openCreate"
      >
        新建队列
      </el-button>
      <el-button
        type="success"
        :disabled="!selectedNodeId"
        @click="openBatch"
      >
        §3.1.2 批量生成
      </el-button>
    </div>

    <el-empty
      v-if="!selectedNodeId"
      description="请先选择节点"
    />
    <el-table
      v-else
      v-loading="loading"
      :data="queues"
      stripe
      border
    >
      <el-table-column
        prop="queueName"
        label="队列名称"
        width="240"
        show-overflow-tooltip
      />
      <el-table-column
        label="通道类型"
        width="120"
      >
        <template #default="{ row }">
          <StatusTag
            :value="row.channelType"
            :mapping="TLQ_CHANNEL_TYPE_MAP"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="队列类型"
        width="120"
      >
        <template #default="{ row }">
          <StatusTag
            :value="row.queueType"
            :mapping="TLQ_QUEUE_TYPE_MAP"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="100"
      >
        <template #default="{ row }">
          <StatusTag
            :value="row.queueStatus"
            :mapping="ENABLE_DISABLE_STATUS_MAP"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        width="180"
      >
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="100"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            @click="confirmDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <TlqQueueCreateDialog
      v-model="createDialogVisible"
      :node-id="selectedNodeId"
      @success="onCreated"
    />
    <TlqQueueBatchGenerateDialog
      v-model="batchDialogVisible"
      :node-id="selectedNodeId"
      @success="onCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { StatusTag } from '@/shared/components';
import {
  ENABLE_DISABLE_STATUS_MAP,
  TLQ_CHANNEL_TYPE_MAP,
  TLQ_QUEUE_TYPE_MAP,
} from '@/shared/types/enum-maps';
import { ALL_NODES_PAGE_SIZE, tlqNodeApi } from '../api/tlq-node-api';
import { tlqQueueApi } from '../api/tlq-queue-api';
import type { TlqNodeResponse, TlqQueueConfigResponse } from '../types';
import TlqQueueCreateDialog from '../components/TlqQueueCreateDialog.vue';
import TlqQueueBatchGenerateDialog from '../components/TlqQueueBatchGenerateDialog.vue';

/**
 * TLQ queues page — §5.7.1 / §3.1.2 (FR-WEB-TLQ-CFG).
 *
 * <p>Node-scoped queue management: a top-level {@code el-select} picks the
 * owning TLQ node; selection triggers {@code tlqQueueApi.listByNode(nodeId)}
 * (non-paginated {@code List<T>} per backend contract). Create / batch-generate
 * dialogs receive the current {@code nodeId} as prop and emit {@code success}
 * to trigger reload.</p>
 *
 * <p>Long queue names (e.g. {@code QSEND.A1000143000104}) fit within a 240px
 * column with {@code show-overflow-tooltip} to avoid layout break.</p>
 *
 * <p>Red line #1 compliance: delete / create / batch-generate are all reached
 * through DOM events or child-component emits; internal state ({@code
 * selectedNodeId}, {@code loadQueues}) is exposed via {@code defineExpose}
 * only for targeted test mutation (node selection simulation), not for
 * behavior bypass.</p>
 */

const nodes = ref<TlqNodeResponse[]>([]);
const selectedNodeId = ref('');
const queues = ref<TlqQueueConfigResponse[]>([]);
const loading = ref(false);
const createDialogVisible = ref(false);
const batchDialogVisible = ref(false);

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-';
  try {
    return new Date(value).toLocaleString('zh-CN');
  } catch {
    return value;
  }
}

async function loadNodes(): Promise<void> {
  try {
    const result = await tlqNodeApi.listNodes({ pageNum: 1, pageSize: ALL_NODES_PAGE_SIZE });
    nodes.value = result.records;
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载节点列表失败';
    ElMessage.error(msg);
  }
}

async function loadQueues(): Promise<void> {
  if (!selectedNodeId.value) {
    queues.value = [];
    return;
  }
  loading.value = true;
  try {
    queues.value = await tlqQueueApi.listByNode(selectedNodeId.value);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '加载队列列表失败';
    ElMessage.error(msg);
  } finally {
    loading.value = false;
  }
}

function onNodeChange(): void {
  void loadQueues();
}

async function confirmDelete(row: TlqQueueConfigResponse): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除队列 ${row.queueName}?`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
  } catch {
    // User dismissed the dialog — not an error.
    return;
  }
  try {
    await tlqQueueApi.deleteQueue(row.queueId);
    ElMessage.success('删除成功');
    await loadQueues();
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '删除失败';
    ElMessage.error(msg);
  }
}

function openCreate(): void {
  createDialogVisible.value = true;
}

function openBatch(): void {
  batchDialogVisible.value = true;
}

function onCreated(): void {
  void loadQueues();
}

onMounted(loadNodes);

// Narrow expose surface: test harness sets selectedNodeId (simulating a user
// picking an option from the el-select popover, which jsdom does not render
// reliably) and invokes loadQueues at the change-boundary. Delete/create are
// driven through DOM button clicks — red line #1 compliant.
defineExpose({ selectedNodeId, loadQueues });
</script>

<style scoped>
.tlq-queues-page {
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
  align-items: center;
  gap: 12px;
}
</style>
