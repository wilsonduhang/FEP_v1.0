<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="onDialogUpdate"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
    >
      <el-form-item
        label="节点名称"
        prop="nodeName"
      >
        <el-input
          v-model="form.nodeName"
          maxlength="100"
          placeholder="1-100 个字符"
        />
      </el-form-item>
      <el-form-item
        label="节点角色"
        prop="nodeRole"
      >
        <el-select
          v-model="form.nodeRole"
          :disabled="isUpdate"
          placeholder="请选择节点角色"
          style="width: 100%"
        >
          <el-option
            v-for="[key, mapping] in roleOptions"
            :key="key"
            :value="key"
            :label="mapping.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="主机 IP"
        prop="hostIp"
      >
        <el-input
          v-model="form.hostIp"
          placeholder="IP 或域名，如 192.168.1.10"
        />
      </el-form-item>
      <el-form-item
        label="监听端口"
        prop="port"
      >
        <el-input-number
          v-model="form.port"
          :min="1"
          :max="65535"
        />
      </el-form-item>
      <el-form-item
        label="VIP 地址"
        prop="vipAddress"
      >
        <el-input
          v-model="form.vipAddress"
          placeholder="可选"
          maxlength="100"
        />
      </el-form-item>
      <el-form-item
        label="通信协议"
        prop="protocol"
      >
        <el-input
          v-model="form.protocol"
          placeholder="默认 TCP"
          maxlength="20"
        />
      </el-form-item>
      <el-form-item
        label="描述"
        prop="description"
      >
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="可选"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="onCancel">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="onSubmit"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { tlqNodeApi } from '../api/tlq-node-api';
import { TLQ_NODE_ROLE_MAP } from '@/shared/types/enum-maps';
import type {
  TlqNodeCreateRequest,
  TlqNodeResponse,
  TlqNodeRole,
  TlqNodeUpdateRequest,
} from '../types';

/**
 * Create/update dialog for TLQ node configuration (PRD §5.7.1, FR-WEB-TLQ-CFG).
 *
 * <p>Seven fields: {@code nodeName / nodeRole / hostIp / port / vipAddress /
 * protocol / description}. In update mode the {@code nodeRole} select is
 * disabled because the backend {@code TlqNodeController} rejects role
 * mutations on PUT (see {@code TlqNodeUpdateRequest} in {@code types.ts}:
 * {@code nodeRole} is intentionally absent from the partial update type).</p>
 *
 * <p>On successful create/update this component emits {@code success} so the
 * parent ({@code TlqNodesPage}) can reload its paginated list, then emits
 * {@code update:modelValue(false)} to close the dialog. Backend errors
 * (e.g. HTTP 409 duplicate name) are surfaced through {@code ElMessage.error}
 * using the error message thrown by {@code httpClient}.</p>
 *
 * <p>Contract stable: props ({@code modelValue / mode / record}) and emits
 * ({@code update:modelValue / success}) match the Task 2 placeholder, keeping
 * the {@code TlqNodesPage} integration intact.</p>
 */

interface Props {
  modelValue: boolean;
  mode: 'create' | 'update';
  record?: TlqNodeResponse | null;
}

const props = withDefaults(defineProps<Props>(), { record: null });

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isUpdate = computed(() => props.mode === 'update');
const dialogTitle = computed(() => (isUpdate.value ? '编辑 TLQ 节点' : '新建 TLQ 节点'));

// Cached to give template a stable iterable reference; Object.entries yields
// fresh arrays per access which would make v-for warnings noisy.
const roleOptions = computed(() => Object.entries(TLQ_NODE_ROLE_MAP));

interface FormModel {
  nodeName: string;
  nodeRole: TlqNodeRole | '';
  hostIp: string;
  port: number;
  vipAddress: string;
  protocol: string;
  description: string;
}

function defaultForm(): FormModel {
  return {
    nodeName: '',
    nodeRole: '',
    hostIp: '',
    port: 20001,
    vipAddress: '',
    protocol: 'TCP',
    description: '',
  };
}

const form = reactive<FormModel>(defaultForm());

const rules: FormRules<FormModel> = {
  nodeName: [
    { required: true, message: '节点名称不能为空', trigger: 'blur' },
    { min: 1, max: 100, message: '长度 1-100 字符', trigger: 'blur' },
  ],
  nodeRole: [
    { required: true, message: '节点角色不能为空', trigger: 'change' },
  ],
  hostIp: [
    { required: true, message: '主机 IP 不能为空', trigger: 'blur' },
    { pattern: /^[\w.-]+$/, message: 'IP 或域名格式不正确', trigger: 'blur' },
  ],
  port: [
    { required: true, message: '端口不能为空', trigger: 'change' },
    { type: 'number', min: 1, max: 65535, message: '端口范围 1-65535', trigger: 'change' },
  ],
  vipAddress: [
    { max: 100, message: 'VIP 地址最长 100 字符', trigger: 'blur' },
  ],
  protocol: [
    { max: 20, message: '协议名最长 20 字符', trigger: 'blur' },
  ],
  description: [
    { max: 500, message: '描述最长 500 字符', trigger: 'blur' },
  ],
};

// Repopulate form whenever the dialog (re)opens or the target record changes.
// Using watch over computed guards against stale state when the parent reuses
// the dialog for a different row without unmounting.
watch(
  () => [props.modelValue, props.record, props.mode] as const,
  ([visible, record, mode]) => {
    if (!visible) return;
    if (mode === 'update' && record) {
      Object.assign(form, {
        nodeName: record.nodeName,
        nodeRole: record.nodeRole,
        hostIp: record.hostIp,
        port: record.port,
        vipAddress: record.vipAddress ?? '',
        protocol: record.protocol,
        description: record.description ?? '',
      } satisfies FormModel);
    } else {
      Object.assign(form, defaultForm());
    }
    // Clear previous validation state so stale errors do not bleed across
    // successive dialog openings.
    formRef.value?.clearValidate();
  },
  { immediate: true },
);

function onDialogUpdate(value: boolean): void {
  emit('update:modelValue', value);
}

function onCancel(): void {
  emit('update:modelValue', false);
}

async function onSubmit(): Promise<void> {
  if (!formRef.value) return;
  // Element Plus FormInstance.validate() rejects with invalidFields when any
  // rule fails; inline errors auto-render via rules.message. Bail silently on
  // reject — EP already surfaces the errors under each field.
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (props.mode === 'create') {
      const payload: TlqNodeCreateRequest = {
        nodeName: form.nodeName,
        // Validation above guarantees nodeRole is a non-empty TlqNodeRole.
        nodeRole: form.nodeRole as TlqNodeRole,
        hostIp: form.hostIp,
        port: form.port,
        vipAddress: form.vipAddress ? form.vipAddress : undefined,
        protocol: form.protocol ? form.protocol : undefined,
        description: form.description ? form.description : undefined,
      };
      await tlqNodeApi.createNode(payload);
      ElMessage.success('节点创建成功');
    } else {
      if (!props.record) return;
      // nodeRole intentionally excluded — backend TlqNodeController rejects
      // role updates, enforced by the TlqNodeUpdateRequest type itself.
      const payload: TlqNodeUpdateRequest = {
        nodeName: form.nodeName,
        hostIp: form.hostIp,
        port: form.port,
        vipAddress: form.vipAddress ? form.vipAddress : undefined,
        protocol: form.protocol ? form.protocol : undefined,
        description: form.description ? form.description : undefined,
      };
      await tlqNodeApi.updateNode(props.record.nodeId, payload);
      ElMessage.success('节点更新成功');
    }
    emit('success');
    emit('update:modelValue', false);
  } catch (err) {
    const message = err instanceof Error ? err.message : '操作失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

// Exposed for unit tests (peer pattern — see OutputInterfaceEditDialog).
defineExpose({ form, rules, formRef, onSubmit, mode: props.mode });
</script>
