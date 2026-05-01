<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  listDirMap,
  updateDirMap,
  listDirMapHistory,
  type DirMapConfig,
  type DirMapHistory,
  type DirMapUpdateRequest,
} from '../api/dir-map-api';

const rows = ref<DirMapConfig[]>([]);
const loading = ref(false);
const editDrawer = ref(false);
const historyDrawer = ref(false);
// T6 quality reviewer P1 修复（2026-04-30）：editRow 与 historyRow 拆分独立 ref。
// 此前 openHistory 复写 editRow，导致用户先在行 A 打开编辑、再点行 B 历史时，
// editRow 被覆盖为行 B；若 edit drawer 仍开着点击 Save，会把行 A 的 form
// 写到行 B 的路径变量上 → 静默写错行（数据正确性 P1）。
const editRow = ref<DirMapConfig | null>(null);
const historyRow = ref<DirMapConfig | null>(null);
const histories = ref<DirMapHistory[]>([]);
const editForm = ref<DirMapUpdateRequest>({
  direction: '',
  requiresFep: true,
  processingMode: '',
  changeReason: '',
});

async function load() {
  loading.value = true;
  try {
    const result = await listDirMap(1, 100);
    rows.value = result.records;
  } catch {
    // T6 quality P2 修复：load 失败时清空，避免渲染陈旧 88 行。
    // 错误 toast 由 httpClient 拦截器统一弹出（client.ts:37/51），不重复弹。
    rows.value = [];
  } finally {
    loading.value = false;
  }
}

function openEdit(row: DirMapConfig) {
  editRow.value = row;
  editForm.value = {
    direction: row.direction,
    requiresFep: row.requiresFep,
    processingMode: row.processingMode,
    changeReason: '',
  };
  editDrawer.value = true;
}

async function saveEdit() {
  if (!editRow.value) return;
  try {
    await updateDirMap(
      editRow.value.messageType,
      editRow.value.accessRole,
      editForm.value,
    );
    ElMessage.success('保存成功，已立即生效');
    editDrawer.value = false;
    await load();
  } catch {
    // T6 quality P2 修复：错误 toast 由 httpClient 拦截器统一弹出（client.ts:37/51
    // 已 ElMessage.error(body.message)），页面层只 catch 防止 unhandled rejection。
    // 此前 `${e.message}` 在 ApiResult plain object reject 路径下永远走 '未知错误'
    // 分支，且会出现双 toast。
  }
}

async function openHistory(row: DirMapConfig) {
  // 先清空再 await，避免 API 失败时残留上次行的历史给用户看。
  historyRow.value = row;
  histories.value = [];
  try {
    histories.value = await listDirMapHistory(row.messageType, row.accessRole);
    historyDrawer.value = true;
  } catch {
    // 错误 toast 由 httpClient 拦截器统一弹出；drawer 不打开，避免误导。
  }
}

onMounted(() => {
  load();
});
</script>

<template>
  <div class="dir-map-page">
    <h2>报文方向映射</h2>
    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      height="600"
    >
      <el-table-column
        prop="messageType"
        label="报文号"
        width="100"
      />
      <el-table-column
        prop="messageName"
        label="报文名称"
        min-width="200"
      />
      <el-table-column
        prop="accessRole"
        label="角色"
        width="160"
      />
      <el-table-column
        prop="direction"
        label="方向"
        width="160"
      />
      <el-table-column
        prop="requiresFep"
        label="需 FEP"
        width="100"
      >
        <template #default="{ row }">
          {{ row.requiresFep ? '是' : '否' }}
        </template>
      </el-table-column>
      <el-table-column
        prop="processingMode"
        label="处理模式"
        width="120"
      />
      <el-table-column
        prop="updatedBy"
        label="更新人"
        width="120"
      />
      <el-table-column
        prop="updatedAt"
        label="更新时间"
        width="180"
      />
      <el-table-column
        label="操作"
        width="160"
      >
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            data-test="btn-edit"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            type="info"
            link
            data-test="btn-history"
            @click="openHistory(row)"
          >
            历史
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer
      v-model="editDrawer"
      title="编辑方向映射"
      size="500px"
    >
      <el-form
        :model="editForm"
        label-width="100px"
      >
        <el-form-item label="方向">
          <el-select
            v-model="editForm.direction"
            data-test="sel-direction"
          >
            <el-option
              value="OUTBOUND_ACTIVE"
              label="主动发起"
            />
            <el-option
              value="INBOUND_PASSIVE"
              label="被动接收"
            />
            <el-option
              value="NOT_APPLICABLE"
              label="不涉及"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="需 FEP">
          <el-switch v-model="editForm.requiresFep" />
        </el-form-item>
        <el-form-item label="处理模式">
          <el-select v-model="editForm.processingMode">
            <el-option
              value="MODE_1"
              label="模式 1"
            />
            <el-option
              value="MODE_2"
              label="模式 2"
            />
            <el-option
              value="MODE_3"
              label="模式 3"
            />
            <el-option
              value="MODE_5"
              label="模式 5"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input
            v-model="editForm.changeReason"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            data-test="btn-save"
            @click="saveEdit"
          >
            保存
          </el-button>
          <el-button @click="editDrawer = false">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-drawer>

    <el-drawer
      v-model="historyDrawer"
      title="变更历史"
      size="700px"
    >
      <el-table
        :data="histories"
        border
      >
        <el-table-column
          prop="changedAt"
          label="时间"
          width="180"
        />
        <el-table-column
          prop="changedBy"
          label="操作人"
          width="120"
        />
        <el-table-column
          prop="oldDirection"
          label="原方向"
          width="160"
        />
        <el-table-column
          prop="newDirection"
          label="新方向"
          width="160"
        />
        <el-table-column
          prop="changeReason"
          label="原因"
          min-width="200"
        />
      </el-table>
    </el-drawer>
  </div>
</template>
