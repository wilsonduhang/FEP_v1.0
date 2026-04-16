<template>
  <el-row
    :gutter="16"
    class="record-summary-cards"
  >
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="card-label">
          总报文数据
        </div>
        <div class="card-value">
          {{ totalCount }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="card-label">
          成功数
        </div>
        <div class="card-value card-success">
          {{ successCount }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="card-label">
          待处理数
        </div>
        <div class="card-value card-danger">
          {{ pendingCount }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <div class="card-label">
          失败数
        </div>
        <div class="card-value card-danger">
          {{ failedCount }}
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RecordSummaryItem } from '../api/biz-message-record-api';

const props = defineProps<{
  items: RecordSummaryItem[];
}>();

const totalCount = computed(() =>
  props.items.reduce((sum, i) => sum + i.totalCount, 0),
);
const successCount = computed(() =>
  props.items.reduce((sum, i) => sum + i.successCount, 0),
);
const pendingCount = computed(() =>
  props.items.reduce((sum, i) => sum + i.pendingCount, 0),
);
const failedCount = computed(() =>
  props.items.reduce((sum, i) => sum + i.failedCount, 0),
);
</script>

<style scoped>
.record-summary-cards { margin-bottom: 8px; }
.card-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.card-value { font-size: 28px; font-weight: 600; }
.card-success { color: #67c23a; }
.card-danger { color: #f56c6c; }
</style>
