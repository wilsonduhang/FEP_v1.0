<!-- src/features/submission/components/RecordDetailDrawer.vue -->
<template>
  <DetailDrawer
    :model-value="modelValue"
    :title="record ? `报送记录详情 — ${record.recordId}` : '报送记录详情'"
    @update:model-value="(v) => $emit('update:modelValue', v)"
  >
    <template v-if="record">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="记录 ID">{{ record.recordId }}</el-descriptions-item>
        <el-descriptions-item label="报文类型">{{ record.messageType }}</el-descriptions-item>
        <el-descriptions-item label="报文名称">{{ record.messageName }}</el-descriptions-item>
        <el-descriptions-item label="业务类型 ID">{{ record.businessTypeId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="报送单位">{{ record.submitterName ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务编号">{{ record.businessNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额(万元)">{{ formatAmount(record.amount) }}</el-descriptions-item>
        <el-descriptions-item label="数据总数">{{ record.dataCount }}</el-descriptions-item>
        <el-descriptions-item label="录入方式">
          <StatusTag :value="record.entryMethod" :mapping="SUB_ENTRY_METHOD_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="录入人">{{ record.entryBy ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="推送状态">
          <StatusTag :value="record.pushStatus" :mapping="PUSH_STATUS_MAP" />
        </el-descriptions-item>
        <el-descriptions-item label="推送时间">{{ formatDateTime(record.pushTime) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(record.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(record.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="错误原因" :span="2">
          <span v-if="record.errorMessage" class="error-msg">{{ record.errorMessage }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </DetailDrawer>
</template>

<script setup lang="ts">
import { DetailDrawer, StatusTag } from '@/shared/components';
import { SUB_ENTRY_METHOD_MAP, PUSH_STATUS_MAP } from '@/shared/types/enum-maps';
import { formatAmount, formatDateTime } from '@/shared/utils/format';
import type { SubmissionRecordResponse } from '../api/sub-report-api';

defineProps<{ modelValue: boolean; record: SubmissionRecordResponse | null }>();
defineEmits<{ (e: 'update:modelValue', v: boolean): void }>();
</script>

<style scoped>
.error-msg { color: #f56c6c; }
</style>
