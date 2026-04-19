<!-- src/features/submission/components/ViewStatsCards.vue -->
<template>
  <div class="view-stats-cards">
    <el-card class="stat-card total" shadow="never">
      <div class="stat-value">{{ totalCount.toLocaleString('en-US') }}</div>
      <div class="stat-label">总数据量</div>
    </el-card>
    <el-card class="stat-card pushed" shadow="never">
      <div class="stat-value">{{ pushedCount.toLocaleString('en-US') }}</div>
      <div class="stat-label">已推送数据量</div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
/**
 * Presentation component for §5.6.3 by-type view page stats cards.
 *
 * <p>Renders two stat cards (total vs pushed data counts) with locale-aware
 * thousand separators. Receives absolute numbers from the parent
 * {@code ReportViewPage}, which derives them from the message summary filter.</p>
 */
defineProps<{ totalCount: number; pushedCount: number }>();
</script>

<style scoped>
.view-stats-cards {
  display: flex;
  gap: 16px;
}
.stat-card {
  flex: 1;
  max-width: 240px;
}
.stat-value {
  font-size: 32px;
  font-weight: 600;
}
.stat-card.total .stat-value {
  color: #409eff;
}
.stat-card.pushed .stat-value {
  color: #67c23a;
}
.stat-label {
  color: #909399;
  font-size: 14px;
  margin-top: 4px;
}
</style>
