<template>
  <el-popover placement="bottom" :width="360" trigger="click" @show="onOpen">
    <template #reference>
      <el-badge :value="store.unread" :hidden="store.unread === 0" class="bell-badge">
        <el-icon :size="20"><Bell /></el-icon>
      </el-badge>
    </template>
    <div class="bell-panel">
      <p v-if="store.items.length === 0" class="empty">暂无未读通知</p>
      <div v-for="n in store.items" :key="n.notificationId" class="bell-item">
        <div class="bell-item-main">
          <strong>{{ n.title }}</strong>
          <span class="bell-msg">{{ n.message }}</span>
        </div>
        <el-button link type="primary" size="small" @click="onRead(n.notificationId)">
          标记已读
        </el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { Bell } from '@element-plus/icons-vue';
import { useNotificationStore } from '../stores/notification-store';

const store = useNotificationStore();

function onOpen() {
  void store.fetchList();
}

function onRead(id: string) {
  void store.markRead(id);
}

onMounted(() => store.startPolling());
onUnmounted(() => store.stopPolling());
</script>

<style scoped>
.bell-badge {
  cursor: pointer;
  margin-right: 20px;
}
.bell-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.bell-item-main {
  display: flex;
  flex-direction: column;
}
.bell-msg {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.empty {
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
