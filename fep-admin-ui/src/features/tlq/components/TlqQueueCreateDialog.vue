<template>
  <el-dialog
    :model-value="modelValue"
    title="新建队列"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="onDialogUpdate"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="建议格式：QLOCAL.HNDEMP / QREMOTE.HNDEMP / QSEND.<机构代码> 等，参考 PRD §3.1.2"
    />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="margin-top: 16px"
    >
      <el-form-item
        label="队列名称"
        prop="queueName"
      >
        <el-input
          v-model="form.queueName"
          maxlength="100"
          placeholder="1-100 个字符，遵循 PRD §3.1.2 命名"
        />
      </el-form-item>
      <el-form-item
        label="通道类型"
        prop="channelType"
      >
        <el-select
          v-model="form.channelType"
          placeholder="请选择通道类型"
          style="width: 100%"
        >
          <el-option
            v-for="[key, mapping] in channelOptions"
            :key="key"
            :value="key"
            :label="mapping.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="队列类型"
        prop="queueType"
      >
        <el-select
          v-model="form.queueType"
          placeholder="请选择队列类型"
          style="width: 100%"
        >
          <el-option
            v-for="[key, mapping] in queueTypeOptions"
            :key="key"
            :value="key"
            :label="mapping.label"
          />
        </el-select>
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
import { tlqQueueApi } from '../api/tlq-queue-api';
import {
  TLQ_CHANNEL_TYPE_MAP,
  TLQ_QUEUE_TYPE_MAP,
} from '@/shared/types/enum-maps';
import type {
  TlqChannelType,
  TlqQueueConfigCreateRequest,
  TlqQueueType,
} from '../types';

/**
 * Single-queue creation dialog for TLQ queue config (PRD §5.7.2 / §3.1.2,
 * FR-WEB-TLQ-CFG).
 *
 * <p>Five fields (+ {@code nodeId} injected from prop): {@code queueName},
 * {@code channelType} (REALTIME/BATCH), {@code queueType} (LOCAL / REMOTE /
 * DEST / SEND / DEAD), {@code description}. A top {@code el-alert} hints at
 * PRD §3.1.2 naming conventions; authoritative naming is enforced by the
 * backend {@code TlqQueueConfigController}.</p>
 *
 * <p>On successful create this component emits {@code success} so the parent
 * ({@code TlqQueuesPage}) can reload its queue list for the current node,
 * then emits {@code update:modelValue(false)} to close the dialog. Backend
 * errors (e.g. HTTP 409 duplicate queue name) surface through
 * {@code ElMessage.error} using the error message thrown by {@code httpClient}.</p>
 *
 * <p>Contract stable vs. Task 4 placeholder: props ({@code modelValue /
 * nodeId}) and emits ({@code update:modelValue / success}) unchanged, so the
 * {@code TlqQueuesPage} integration remains intact.</p>
 */

interface Props {
  modelValue: boolean;
  nodeId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

// Cached iterable references — Object.entries yields fresh arrays per access
// which would make v-for warnings noisy.
const channelOptions = computed(() => Object.entries(TLQ_CHANNEL_TYPE_MAP));
const queueTypeOptions = computed(() => Object.entries(TLQ_QUEUE_TYPE_MAP));

interface FormModel {
  queueName: string;
  channelType: TlqChannelType | '';
  queueType: TlqQueueType | '';
  description: string;
}

function defaultForm(): FormModel {
  return {
    queueName: '',
    channelType: '',
    queueType: '',
    description: '',
  };
}

const form = reactive<FormModel>(defaultForm());

const rules: FormRules<FormModel> = {
  queueName: [
    { required: true, message: '队列名称不能为空', trigger: 'blur' },
    { min: 1, max: 100, message: '长度 1-100 字符', trigger: 'blur' },
  ],
  channelType: [
    { required: true, message: '通道类型不能为空', trigger: 'change' },
  ],
  queueType: [
    { required: true, message: '队列类型不能为空', trigger: 'change' },
  ],
  description: [
    { max: 500, message: '描述最长 500 字符', trigger: 'blur' },
  ],
};

// Reset the form each time the dialog (re)opens so stale fields from a prior
// open do not leak across successive invocations. {@code immediate: true}
// guarantees the reset runs on the mounted render, matching the peer
// {@code TlqNodeEditDialog} watch pattern and keeping jsdom mounts
// deterministic.
watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return;
    Object.assign(form, defaultForm());
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
  // rule fails; EP auto-renders messages under each field. Bail silently on
  // reject to avoid duplicate error surfacing.
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const payload: TlqQueueConfigCreateRequest = {
      nodeId: props.nodeId,
      queueName: form.queueName,
      // Validation above guarantees non-empty channel/queue types.
      channelType: form.channelType as TlqChannelType,
      queueType: form.queueType as TlqQueueType,
      // Empty optional string elided so backend treats the field as unset.
      description: form.description ? form.description : undefined,
    };
    await tlqQueueApi.createQueue(payload);
    ElMessage.success('队列创建成功');
    emit('success');
    emit('update:modelValue', false);
  } catch (err) {
    const message = err instanceof Error ? err.message : '创建失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

// Exposed for unit tests (peer pattern — see TlqNodeEditDialog).
// Deliberately does NOT expose `onSubmit` / `onCancel` — tests must drive the
// 确定 / 取消 buttons via DOM click per Global Test Red Line #1.
defineExpose({ form, rules, formRef });
</script>
