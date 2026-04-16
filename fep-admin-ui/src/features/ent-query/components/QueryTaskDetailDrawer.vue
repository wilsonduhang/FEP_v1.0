<template>
  <DetailDrawer
    :model-value="modelValue"
    :title="`查询任务详情 - ${taskId}`"
    size="60%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="loading" class="loading"><el-icon class="is-loading"><Loading /></el-icon></div>
    <div v-else-if="task" class="content">
      <el-descriptions :column="2" border title="任务信息">
        <el-descriptions-item label="任务 ID">{{ task.taskId }}</el-descriptions-item>
        <el-descriptions-item label="企业 ID">{{ task.enterpriseId }}</el-descriptions-item>
        <el-descriptions-item label="查询类型">
          <StatusTag :value="task.queryType" :mapping="QUERY_TYPE_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="USCI">{{ task.usci }}</el-descriptions-item>
        <el-descriptions-item label="被查询企业">{{ task.queryTargetName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :value="task.taskStatus" :mapping="QUERY_TASK_STATUS_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ task.createTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.completeTime ?? '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="task.errorMessage" label="错误消息" :span="2">
          <span class="error-text">{{ task.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="task.taskStatus === 'PROCESSING'"
        class="processing-hint"
        title="任务处理中，结果尚未返回"
        type="warning"
        show-icon
        :closable="false"
      />

      <h3 class="section-title">查询结果</h3>
      <el-empty v-if="results.length === 0" description="暂无结果数据" />
      <el-table v-else :data="results" :row-class-name="rowClassName" stripe border>
        <el-table-column prop="resultId" label="结果 ID" min-width="120" />
        <el-table-column prop="resultUsci" label="USCI" min-width="180" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :value="row.resultStatus" :mapping="RESULT_STATUS_MAP" />
          </template>
        </el-table-column>
        <el-table-column prop="errorCode" label="错误码" width="120" />
        <el-table-column prop="errorMessage" label="错误消息" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
      </el-table>
    </div>
  </DetailDrawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { DetailDrawer, StatusTag } from '@/shared/components';
import { QUERY_TYPE_MAP, QUERY_TASK_STATUS_MAP, RESULT_STATUS_MAP } from '@/shared/types/enum-maps';
import {
  entQueryTaskApi,
  type QueryTaskResponse,
  type QueryResultResponse,
} from '../api/ent-query-task-api';

const props = defineProps<{ modelValue: boolean; taskId: string }>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();

const task = ref<QueryTaskResponse | null>(null);
const results = ref<QueryResultResponse[]>([]);
const loading = ref(false);

async function refresh() {
  if (!props.taskId) return;
  loading.value = true;
  try {
    const [t, r] = await Promise.all([
      entQueryTaskApi.getById(props.taskId),
      entQueryTaskApi.listResults(props.taskId),
    ]);
    task.value = t;
    results.value = r;
  } finally {
    loading.value = false;
  }
}

function rowClassName({ row }: { row: QueryResultResponse }): string {
  return row.resultStatus === 'ERROR' ? 'error-row' : '';
}

watch(
  () => [props.modelValue, props.taskId],
  ([open]) => { if (open) refresh(); },
  { immediate: true },
);
</script>

<style scoped>
.loading { display: flex; justify-content: center; padding: 40px; }
.content { display: flex; flex-direction: column; gap: 16px; }
.section-title { margin: 16px 0 8px; font-size: 16px; font-weight: 600; }
.processing-hint { margin-top: 8px; }
.error-text { color: #f56c6c; }
:deep(.error-row) { background-color: #fef0f0; }
</style>
