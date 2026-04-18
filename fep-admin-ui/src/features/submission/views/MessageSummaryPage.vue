<template>
  <div class="message-summary-page">
    <div class="header">
      <h2>报文数据列表</h2>
      <el-button
        type="primary"
        size="small"
        data-test="manual-refresh"
        @click="loadSummary"
      >
        刷新
      </el-button>
    </div>
    <SubMessageSummaryCards
      :items="items"
      @navigate="onNavigate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import SubMessageSummaryCards from '../components/SubMessageSummaryCards.vue';
import {
  subMessageSummaryApi,
  type MessageSummaryItem,
} from '../api/sub-message-summary-api';

const router = useRouter();
const items = ref<MessageSummaryItem[]>([]);

async function loadSummary() {
  try {
    items.value = await subMessageSummaryApi.getSummary();
  } catch {
    ElMessage.error('报文汇总加载失败');
  }
}

function onNavigate({ messageType }: { messageType: string }) {
  router.push({ path: '/report/records', query: { messageType } });
}

onMounted(loadSummary);

defineExpose({ loadSummary, onNavigate });
</script>

<style scoped>
.message-summary-page {
  padding: 24px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
