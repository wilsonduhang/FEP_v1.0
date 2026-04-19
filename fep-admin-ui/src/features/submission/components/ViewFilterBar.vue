<!-- src/features/submission/components/ViewFilterBar.vue -->
<template>
  <div class="view-filter-bar">
    <el-form :inline="true" :model="filter" size="small">
      <el-form-item label="数据类型">
        <el-select v-model="filter.dataType" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option
            v-for="opt in props.dataTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="报送类型">
        <el-select v-model="filter.reportType" placeholder="全部" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="接口调取" value="API_CALL" />
          <el-option label="手动录入" value="MANUAL_ENTRY" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="filter.dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          start-placeholder="起始"
          end-placeholder="截止"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="apply">筛选</el-button>
        <el-button @click="reset">重置</el-button>
        <MockBadge size="small" style="margin-left: 8px">服务端过滤 P1 启用</MockBadge>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { MockBadge } from '@/shared/components';

/**
 * Three-axis filter bar for §5.6.3 by-type view page (v1b P0-δ).
 *
 * <p>Axes:</p>
 * <ul>
 *   <li>dataType — optional dropdown (parent supplies options; empty array
 *       renders only the "全部" placeholder entry)</li>
 *   <li>reportType — AUTO (API_CALL) / MANUAL (MANUAL_ENTRY)</li>
 *   <li>dateRange — datetime range with ISO 8601 value format</li>
 * </ul>
 *
 * <p>The component emits {@code apply} with the current filter state. Backend
 * filtering is not yet supported (TLQ pending); the parent applies the filter
 * in-memory via {@code filteredRecords} computed, and the MockBadge signals
 * that server-side filtering will land with P1.</p>
 */
export interface FilterState {
  dataType: string;
  reportType: '' | 'API_CALL' | 'MANUAL_ENTRY';
  dateRange: [string, string] | null;
}

const emit = defineEmits<{ (e: 'apply', v: FilterState): void }>();
const props = withDefaults(
  defineProps<{ dataTypeOptions?: Array<{ value: string; label: string }> }>(),
  { dataTypeOptions: () => [] },
);

const filter = reactive<FilterState>({ dataType: '', reportType: '', dateRange: null });

function apply(): void {
  emit('apply', { ...filter });
}

function reset(): void {
  filter.dataType = '';
  filter.reportType = '';
  filter.dateRange = null;
  emit('apply', { ...filter });
}

defineExpose({ filter, apply, reset });
</script>

<style scoped>
.view-filter-bar {
  padding: 12px;
  background: #fafafa;
  border: 1px solid #eaeaea;
  border-radius: 4px;
}
</style>
