<template>
  <div class="data-table">
    <el-table
      v-loading="loading"
      :data="data"
      stripe
      border
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
      >
        <template
          v-if="col.slot"
          #default="scope"
        >
          <slot
            :name="col.slot"
            v-bind="scope"
          />
        </template>
      </el-table-column>
      <slot name="operation" />
    </el-table>
    <el-pagination
      class="pager"
      :total="total"
      :current-page="pageNum"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @update:current-page="emit('update:pageNum', $event)"
      @update:page-size="emit('update:pageSize', $event)"
    />
  </div>
</template>

<script setup lang="ts" generic="T">
export interface DataTableColumn {
  prop: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  slot?: string;
}

defineProps<{
  data: T[];
  columns: DataTableColumn[];
  loading?: boolean;
  total: number;
  pageNum: number;
  pageSize: number;
}>();

const emit = defineEmits<{
  'update:pageNum': [value: number];
  'update:pageSize': [value: number];
}>();
</script>

<style scoped>
.data-table { display: flex; flex-direction: column; gap: 12px; }
.pager { justify-content: flex-end; }
</style>
