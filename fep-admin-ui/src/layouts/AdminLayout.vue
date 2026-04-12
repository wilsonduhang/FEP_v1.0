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
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
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
import { HomeFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

async function onLogout() {
  await authStore.logout();
  router.push({ name: 'Login' });
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.aside { background: #1f2d5a; color: #fff; }
.logo { padding: 16px; font-weight: 600; color: #fff; }
.header { display: flex; justify-content: flex-end; align-items: center; gap: 12px; background: #fff; border-bottom: 1px solid #eaeaea; }
.user { font-size: 14px; color: #333; }
</style>
