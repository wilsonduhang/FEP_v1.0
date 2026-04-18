<template>
  <div
    v-if="items.length === 0"
    class="empty"
  >
    暂无数据
  </div>
  <el-row
    v-else
    :gutter="16"
  >
    <el-col
      v-for="item in items"
      :key="item.messageType"
      :xs="24"
      :sm="12"
      :md="8"
      :lg="6"
    >
      <el-card
        class="summary-card"
        shadow="hover"
        @click="emit('navigate', { messageType: item.messageType })"
      >
        <div class="card-title">
          {{ item.messageType }} · {{ item.messageName }}
        </div>
        <div class="card-meta">
          业务类型：{{ item.businessTypeId }}
        </div>
        <div class="card-stats">
          <div>总数：<strong>{{ item.totalCount }}</strong></div>
          <div>已推送：<strong>{{ item.pushedCount }}</strong></div>
          <div>待推送：<strong>{{ item.totalCount - item.pushedCount }}</strong></div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import type { MessageSummaryItem } from '../api/sub-message-summary-api';

defineProps<{ items: MessageSummaryItem[] }>();
const emit = defineEmits<{
  navigate: [payload: { messageType: string }];
}>();
</script>

<style scoped>
.summary-card {
  cursor: pointer;
  margin-bottom: 12px;
}
.card-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 15px;
}
.card-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-bottom: 12px;
}
.card-stats div {
  margin-bottom: 4px;
}
.empty {
  padding: 48px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
