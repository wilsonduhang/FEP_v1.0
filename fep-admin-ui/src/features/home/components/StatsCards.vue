<template>
  <el-row :gutter="16">
    <el-col :span="6">
      <el-card class="stat-card">
        <div class="label">
          累计交易额
        </div>
        <div class="value primary">
          {{ cards?.totalAmount ?? '-' }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card class="stat-card">
        <div class="label">
          成功笔数
        </div>
        <div class="value success">
          {{ cards?.successCount ?? 0 }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card class="stat-card">
        <div class="label">
          今日报文量
        </div>
        <div class="value info">
          {{ cards?.todayMessageCount ?? 0 }}
        </div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card class="stat-card">
        <div class="label">
          异常数
        </div>
        <div class="value danger">
          {{ cards?.exceptionCount ?? 0 }}
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { statsApi, type StatsCardsResponse } from '@/features/home/api/stats-api';

const cards = ref<StatsCardsResponse | null>(null);

async function load() {
  try {
    cards.value = await statsApi.getCards();
  } catch {
    cards.value = null;
  }
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-card .label { font-size: 13px; color: #888; margin-bottom: 8px; }
.stat-card .value { font-size: 28px; font-weight: 600; }
.stat-card .value.primary { color: #409eff; }
.stat-card .value.success { color: #67c23a; }
.stat-card .value.info { color: #909399; }
.stat-card .value.danger { color: #f56c6c; }
</style>
