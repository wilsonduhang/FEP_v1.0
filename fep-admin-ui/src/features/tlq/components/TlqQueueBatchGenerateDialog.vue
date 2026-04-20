<template>
  <el-dialog
    :model-value="modelValue"
    title="§3.1.2 批量生成标准队列"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="将按 PRD §3.1.2 规范为当前节点生成 9 条标准队列（已存在的自动跳过）"
    />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="margin-top: 16px"
    >
      <el-form-item
        label="机构代码"
        prop="organizationCode"
      >
        <el-input
          v-model="form.organizationCode"
          maxlength="50"
          placeholder="HNDEMP 中心代码"
        />
        <div class="hint">默认 <code>A1000143000104</code>（HNDEMP 中心节点，PRD §3.1.2）</div>
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
        生成
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { tlqQueueApi } from '../api/tlq-queue-api';
import type { TlqQueueBatchGenerateRequest } from '../types';

/**
 * Batch-generate dialog for TLQ queue config (PRD §5.7 / §3.1.2,
 * FR-WEB-TLQ-CFG).
 *
 * <p>Single field ({@code organizationCode}, + {@code nodeId} injected from
 * prop): defaults to HNDEMP center code {@code A1000143000104} (PRD §3.1.2
 * fixed value, overridable by user). Top {@code el-alert} hints at the 9
 * standard queues to be generated.</p>
 *
 * <p>On successful generation with {@code response.length > 0} this component
 * emits {@code success} so the parent ({@code TlqQueuesPage}) can reload its
 * queue list for the current node, then emits {@code update:modelValue(false)}
 * to close the dialog. If {@code response.length === 0} (all queues exist),
 * an info message is shown instead of success. Backend errors surface through
 * {@code ElMessage.error}.</p>
 *
 * <p>Contract stable vs. Task 4 placeholder: props ({@code modelValue /
 * nodeId}) and emits ({@code update:modelValue / success}) unchanged, so the
 * {@code TlqQueuesPage} integration remains intact.</p>
 */

const DEFAULT_HNDEMP_CODE = 'A1000143000104';

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

interface FormModel {
  organizationCode: string;
}

function defaultForm(): FormModel {
  return {
    organizationCode: DEFAULT_HNDEMP_CODE,
  };
}

const form = reactive<FormModel>(defaultForm());

const rules: FormRules<FormModel> = {
  organizationCode: [
    { required: true, message: '机构代码不能为空', trigger: 'blur' },
    { min: 1, max: 50, message: '长度 1-50 字符', trigger: 'blur' },
  ],
};

// Reset the form each time the dialog (re)opens so the default HNDEMP code is
// restored if the user had modified it in a prior open.
watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return;
    Object.assign(form, defaultForm());
    formRef.value?.clearValidate();
  },
  { immediate: true },
);

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
    const payload: TlqQueueBatchGenerateRequest = {
      nodeId: props.nodeId,
      organizationCode: form.organizationCode,
    };
    const created = await tlqQueueApi.batchGenerate(payload);
    if (created.length === 0) {
      ElMessage.info('所有标准队列均已存在，无需新建');
    } else {
      ElMessage.success(`已创建 ${created.length} 条队列`);
    }
    emit('success');
    emit('update:modelValue', false);
  } catch (err) {
    const message = err instanceof Error ? err.message : '批量生成失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

// Exposed for unit tests (peer pattern — see TlqQueueCreateDialog).
// Deliberately does NOT expose `onSubmit` / `onCancel` — tests must drive the
// 生成 / 取消 buttons via DOM click per Global Test Red Line #1.
defineExpose({ form, rules, formRef });
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
