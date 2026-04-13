<template>
  <el-card class="todo-list-card">
    <template #header>
      <div class="header">
        <span>待办事项</span>
        <el-button
          size="small"
          @click="load"
        >
          刷新
        </el-button>
      </div>
    </template>
    <el-table
      v-loading="loading"
      :data="items"
      empty-text="暂无待办"
    >
      <el-table-column
        prop="title"
        label="标题"
      />
      <el-table-column
        prop="taskType"
        label="类型"
        width="140"
      />
      <el-table-column
        label="优先级"
        width="100"
      >
        <template #default="{ row }">
          <el-tag :type="priorityTag(row.priority)">
            {{ row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="createTime"
        label="创建时间"
        width="180"
      />
      <el-table-column
        label="操作"
        width="220"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            class="btn-start"
            :disabled="row.todoStatus !== 'PENDING'"
            @click="onStart(row)"
          >
            开始处理
          </el-button>
          <el-button
            size="small"
            type="success"
            :disabled="row.todoStatus === 'COMPLETED'"
            @click="onComplete(row)"
          >
            完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 0"
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="onPageChange"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  todoApi,
  type TodoPriority,
  type TodoResponse,
} from '@/features/home/api/todo-api';

const emit = defineEmits<{ (e: 'update'): void }>();

const items = ref<TodoResponse[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

function priorityTag(p: TodoPriority): 'danger' | 'warning' | 'primary' | 'info' {
  switch (p) {
    case 'URGENT':
      return 'danger';
    case 'HIGH':
      return 'warning';
    case 'MEDIUM':
      return 'primary';
    case 'LOW':
      return 'info';
  }
}

async function load() {
  loading.value = true;
  try {
    const result = await todoApi.search({ pageNum: pageNum.value, pageSize: pageSize.value });
    items.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function onPageChange(p: number) {
  pageNum.value = p;
  load();
}

async function onStart(row: TodoResponse) {
  await todoApi.startProcessing(row.todoId);
  emit('update');
  await load();
}

async function onComplete(row: TodoResponse) {
  await todoApi.complete(row.todoId);
  emit('update');
  await load();
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
.todo-list-card .header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.btn-start {
  margin-right: 8px;
}
</style>
