<!--
  ShortcutGrid — per-user shortcut grid on the home dashboard.
  PRD v1.3 §5.2.4. Only renders visible === true items, supports
  up/down reordering and per-item visibility toggle.
-->
<template>
  <el-card>
    <template #header>
      快捷入口
    </template>
    <div class="shortcut-grid">
      <div
        v-for="(item, idx) in visibleItems"
        :key="item.shortcutId"
        class="shortcut-item"
      >
        <a
          :href="item.targetUrl"
          class="link"
        >
          <div class="icon-wrap">
            <el-icon :size="28">
              <Menu />
            </el-icon>
          </div>
          <div class="name">
            {{ item.shortcutName }}
          </div>
        </a>
        <div class="actions">
          <el-button
            size="small"
            circle
            class="btn-move-up"
            :disabled="idx === 0"
            @click="moveUp(idx)"
          >
            ↑
          </el-button>
          <el-button
            size="small"
            circle
            class="btn-move-down"
            :disabled="idx === visibleItems.length - 1"
            @click="moveDown(idx)"
          >
            ↓
          </el-button>
          <el-button
            size="small"
            circle
            class="btn-hide"
            @click="onToggle(item)"
          >
            ×
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Menu } from '@element-plus/icons-vue';
import {
  shortcutApi,
  type ShortcutResponse,
} from '@/features/home/api/shortcut-api';

const items = ref<ShortcutResponse[]>([]);

const visibleItems = computed(() =>
  items.value
    .filter((i) => i.visible)
    .sort((a, b) => a.sortOrder - b.sortOrder),
);

async function load() {
  try {
    items.value = await shortcutApi.list();
  } catch {
    items.value = [];
  }
}

async function commitReorder() {
  await shortcutApi.reorder({
    items: visibleItems.value.map((item, idx) => ({
      shortcutId: item.shortcutId,
      sortOrder: idx + 1,
    })),
  });
  await load();
}

async function moveUp(idx: number) {
  if (idx === 0) {
    return;
  }
  const list = [...visibleItems.value];
  [list[idx - 1], list[idx]] = [list[idx], list[idx - 1]];
  list.forEach((item, i) => {
    item.sortOrder = i + 1;
  });
  items.value = items.value.map(
    (it) => list.find((x) => x.shortcutId === it.shortcutId) ?? it,
  );
  await commitReorder();
}

async function moveDown(idx: number) {
  if (idx >= visibleItems.value.length - 1) {
    return;
  }
  const list = [...visibleItems.value];
  [list[idx + 1], list[idx]] = [list[idx], list[idx + 1]];
  list.forEach((item, i) => {
    item.sortOrder = i + 1;
  });
  items.value = items.value.map(
    (it) => list.find((x) => x.shortcutId === it.shortcutId) ?? it,
  );
  await commitReorder();
}

async function onToggle(item: ShortcutResponse) {
  await shortcutApi.toggleVisibility(item.shortcutId);
  await load();
}

defineExpose({ load });
onMounted(load);
</script>

<style scoped>
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.shortcut-item {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 12px;
  text-align: center;
  position: relative;
}
.shortcut-item .link {
  display: block;
  color: inherit;
  text-decoration: none;
}
.shortcut-item .icon-wrap {
  margin-bottom: 8px;
  color: #409eff;
}
.shortcut-item .name {
  font-size: 14px;
}
.shortcut-item .actions {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 4px;
}
</style>
