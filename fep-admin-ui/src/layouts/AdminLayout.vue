<template>
  <el-container class="admin-layout">
    <el-aside
      width="220px"
      class="aside"
    >
      <div class="logo">
        FEP 综合前置平台
      </div>
      <el-menu
        :default-active="$route.path"
        router
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <template
          v-for="node in menuTree"
          :key="node.menuId"
        >
          <el-sub-menu
            v-if="node.children.length > 0"
            :index="node.menuCode"
          >
            <template #title>
              {{ node.menuName }}
            </template>
            <el-menu-item
              v-for="leaf in node.children"
              :key="leaf.menuId"
              :index="leaf.routePath || leaf.menuCode"
            >
              {{ leaf.menuName }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item
            v-else
            :index="node.routePath || node.menuCode"
          >
            {{ node.menuName }}
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <NotificationBell />
        <span class="user">{{ authStore.profile?.userName ?? '未登录' }}</span>
        <el-button
          link
          type="primary"
          @click="onLogout"
        >
          退出
        </el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { HomeFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import NotificationBell from '@/features/callback/components/NotificationBell.vue';

const authStore = useAuthStore();
const router = useRouter();

const menuTree = computed(() => authStore.profile?.menuTree ?? []);

async function onLogout() {
  await authStore.logout();
  router.push({ name: 'Login' });
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.aside { background: #f5f5f5; border-right: 1px solid #e6e6e6; }
.logo { padding: 16px; font-weight: 600; color: #333; font-size: 15px; }
.header { display: flex; justify-content: flex-end; align-items: center; gap: 12px; background: #fff; border-bottom: 1px solid #eaeaea; }
.user { font-size: 14px; color: #333; }

/* el-menu 背景对齐侧栏浅灰 */
:deep(.el-menu) { background-color: #f5f5f5; border-right: none; }
:deep(.el-sub-menu__title) { color: #333; }
:deep(.el-menu-item) { color: #333; }
:deep(.el-menu-item.is-active) { color: #409eff; background-color: #e6f0ff; }
</style>
