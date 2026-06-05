<template>
  <el-dialog
    :model-value="modelValue"
    title="重放链"
    width="720px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-table v-loading="loading" :data="chain" border>
      <el-table-column prop="queueId" label="队列 ID" min-width="200" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="retryCount" label="重试次数" width="90" />
      <el-table-column prop="replayedBy" label="重放人" width="120" />
      <el-table-column label="重放时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.replayedAt) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && chain.length === 0" description="无重放链记录" />
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { callbackDlqApi, type DlqEntryResponse } from '../api/callbackDlq';
import { formatDateTime } from '@/shared/utils/format';

const props = defineProps<{ modelValue: boolean; queueId: string | null }>();
const emit = defineEmits<{ 'update:modelValue': [boolean] }>();

const chain = ref<DlqEntryResponse[]>([]);
const loading = ref(false);

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.queueId) {
      chain.value = [];
      return;
    }
    loading.value = true;
    try {
      chain.value = await callbackDlqApi.chain(props.queueId);
    } finally {
      loading.value = false;
    }
  },
);
</script>
