<template>
  <div class="credential-page">
    <div class="toolbar">
      <h3>回调凭证管理</h3>
      <el-button type="primary" @click="openCreate">新建凭证</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="interfaceId" label="接口 ID" min-width="160" />
      <el-table-column label="鉴权类型" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.authType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证状态" min-width="260">
        <template #default="{ row }">
          <el-tag v-if="row.tokenConfigured" type="success" size="small">Token 已配置</el-tag>
          <el-tag v-if="row.oauthClientIdConfigured" type="success" size="small">
            ClientId 已配置
          </el-tag>
          <el-tag v-if="row.oauthClientSecretConfigured" type="success" size="small">
            Secret 已配置
          </el-tag>
          <el-tag
            v-if="
              !row.tokenConfigured &&
              !row.oauthClientIdConfigured &&
              !row.oauthClientSecretConfigured
            "
            type="info"
            size="small"
          >
            未配置
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rotatedAt" label="末次轮换" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row.interfaceId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <CredentialEditDialog v-model="dialogVisible" :record="editing" @saved="refresh" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  callbackCredentialApi,
  type CallbackCredentialResponse,
} from '../api/callbackCredential';
import CredentialEditDialog from '../components/CredentialEditDialog.vue';

const rows = ref<CallbackCredentialResponse[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const editing = ref<CallbackCredentialResponse | null>(null);

async function refresh() {
  loading.value = true;
  try {
    rows.value = await callbackCredentialApi.list();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  dialogVisible.value = true;
}

function onEdit(row: CallbackCredentialResponse) {
  editing.value = row;
  dialogVisible.value = true;
}

async function onDelete(interfaceId: string) {
  await ElMessageBox.confirm(`确认删除接口 ${interfaceId} 的凭证？`, '提示', { type: 'warning' });
  await callbackCredentialApi.delete(interfaceId);
  ElMessage.success('已删除');
  refresh();
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
</style>
