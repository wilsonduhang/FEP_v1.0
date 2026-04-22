<template>
  <DetailDrawer
    v-model="visible"
    title="报文记录详情"
    size="50%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="record">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="记录 ID">
          {{ record.recordId }}
        </el-descriptions-item>
        <el-descriptions-item label="报文编码">
          {{ record.messageCode }}
        </el-descriptions-item>
        <el-descriptions-item label="流水号">
          {{ record.serialNo }}
        </el-descriptions-item>
        <el-descriptions-item label="方向">
          {{ record.direction }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ record.processStatus }}
        </el-descriptions-item>
        <el-descriptions-item label="录入方式">
          {{ record.entryMethod }}
        </el-descriptions-item>
        <el-descriptions-item label="发送节点">
          {{ record.senderNode ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="接收节点">
          {{ record.receiverNode ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="业务编号">
          {{ record.businessNo ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          {{ record.amount ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="访问次数">
          {{ record.accessCount }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间">
          {{ record.processTime ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ record.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ record.updateTime }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="record.processStatus === 'FAILED' && record.errorMessage" class="error-section">
        <h4>错误信息</h4>
        <p class="error-message">
          {{ record.errorMessage }}
        </p>
      </div>

      <div v-if="record.xmlContent" class="xml-section">
        <h4>报文内容</h4>
        <pre class="xml-preview">{{ record.xmlContent }}</pre>
      </div>
    </template>
  </DetailDrawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { DetailDrawer } from '@/shared/components';
import { bizMessageRecordApi, type RecordResponse } from '../api/biz-message-record-api';

const props = defineProps<{
  modelValue: boolean;
  recordId: string;
}>();
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>();

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const record = ref<RecordResponse | null>(null);

async function loadRecord(id: string) {
  if (!id) return;
  record.value = await bizMessageRecordApi.getById(id);
}

watch(
  () => [props.modelValue, props.recordId] as const,
  ([open, id]) => {
    if (open && id) {
      loadRecord(id);
    } else if (!open) {
      record.value = null;
    }
  },
  { immediate: true },
);
</script>

<style scoped>
.error-section {
  margin-top: 16px;
}
.error-message {
  color: #f56c6c;
  white-space: pre-wrap;
}
.xml-section {
  margin-top: 16px;
}
.xml-preview {
  font-family: monospace;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  overflow: auto;
  max-height: 400px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
