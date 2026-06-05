<template>
  <div class="dlq-page">
    <div class="toolbar">
      <h3>回调死信队列</h3>
      <el-button @click="refresh">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="queueId" label="队列 ID" min-width="200" />
      <el-table-column prop="targetInterfaceId" label="目标接口" min-width="140" />
      <el-table-column prop="msgNo" label="报文号" width="100" />
      <el-table-column prop="retryCount" label="重试次数" width="90" />
      <el-table-column prop="lastError" label="末次错误" min-width="220" show-overflow-tooltip />
      <el-table-column prop="originalDlqId" label="源死信 ID" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReplay(row)">重放</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-button :disabled="page === 0" @click="prev">上一页</el-button>
      <span class="page-no">第 {{ page + 1 }} 页</span>
      <el-button :disabled="!hasNext" @click="next">下一页</el-button>
    </div>

    <DlqReplayConfirmDialog v-model="dialogVisible" :record="replaying" @replayed="refresh" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { callbackDlqApi, type DlqEntryResponse } from '../api/callbackDlq';
import DlqReplayConfirmDialog from '../components/DlqReplayConfirmDialog.vue';

const SIZE = 20;
const rows = ref<DlqEntryResponse[]>([]);
const loading = ref(false);
const page = ref(0);
const dialogVisible = ref(false);
const replaying = ref<DlqEntryResponse | null>(null);

const hasNext = computed(() => rows.value.length === SIZE);

async function refresh() {
  loading.value = true;
  try {
    rows.value = await callbackDlqApi.list({ page: page.value, size: SIZE });
  } finally {
    loading.value = false;
  }
}

function prev() {
  if (page.value > 0) {
    page.value -= 1;
    refresh();
  }
}

function next() {
  if (hasNext.value) {
    page.value += 1;
    refresh();
  }
}

function openReplay(row: DlqEntryResponse) {
  replaying.value = row;
  dialogVisible.value = true;
}

onMounted(refresh);
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}
</style>
