<template>
  <el-dialog
    :model-value="modelValue"
    title="复制重放确认"
    width="520px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-descriptions v-if="record" :column="1" border>
      <el-descriptions-item label="队列 ID">{{ record.queueId }}</el-descriptions-item>
      <el-descriptions-item label="目标接口">{{ record.targetInterfaceId }}</el-descriptions-item>
      <el-descriptions-item label="报文号">{{ record.msgNo }}</el-descriptions-item>
      <el-descriptions-item label="重试次数">{{ record.retryCount }}</el-descriptions-item>
      <el-descriptions-item label="末次错误">{{ record.lastError }}</el-descriptions-item>
    </el-descriptions>
    <el-alert
      type="info"
      :closable="false"
      title="重放将以该死信行为模板新建 PENDING 行重新投递；原死信行保留作审计证据。"
      style="margin-top: 12px"
    />
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="onConfirm">确认重放</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { callbackDlqApi, type DlqEntryResponse } from '../api/callbackDlq';

const props = defineProps<{ modelValue: boolean; record: DlqEntryResponse | null }>();
const emit = defineEmits<{ 'update:modelValue': [boolean]; replayed: [] }>();

const submitting = ref(false);

async function onConfirm() {
  if (!props.record) return;
  submitting.value = true;
  try {
    const resp = await callbackDlqApi.replay(props.record.queueId);
    ElMessage.success(`已重放，新队列 ID：${resp.newQueueId}`);
    emit('replayed');
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}
</script>
