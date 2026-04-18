<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="600px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item
        label="接口名称"
        prop="interfaceName"
      >
        <el-input
          v-model="form.interfaceName"
          placeholder="1-30 个字符"
          maxlength="30"
        />
      </el-form-item>
      <el-form-item
        label="接口地址"
        prop="interfaceUrl"
      >
        <el-input
          v-model="form.interfaceUrl"
          placeholder="http:// 或 https:// 开头"
        />
      </el-form-item>
      <el-form-item
        label="业务类型"
        prop="businessTypeId"
      >
        <el-select
          v-model="form.businessTypeId"
          placeholder="请选择（可选）"
          clearable
          style="width: 100%"
        >
          <el-option
            v-for="opt in DEFAULT_BIZ_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="鉴权类型"
        prop="authType"
      >
        <el-select
          v-model="form.authType"
          placeholder="请选择"
          style="width: 100%"
        >
          <el-option
            label="Token"
            value="TOKEN"
          />
          <el-option
            label="OAuth2"
            value="OAUTH2"
          />
          <el-option
            label="无"
            value="NONE"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="超时秒数"
        prop="timeoutSeconds"
      >
        <el-input-number
          v-model="form.timeoutSeconds"
          :min="1"
          :max="300"
        />
      </el-form-item>
      <el-form-item
        label="重试次数"
        prop="retryCount"
      >
        <el-input-number
          v-model="form.retryCount"
          :min="0"
          :max="10"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="onSave"
      >
        {{ isCreate ? '创建' : '保存' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import type {
  InterfaceAuthType,
  OutputInterfaceRequest,
  OutputInterfaceResponse,
} from '../api/sub-output-interface-api';
import { DEFAULT_BIZ_TYPE_OPTIONS } from '@/features/biz-data/constants/biz-type-options';

/**
 * Edit/create dialog for FR-WEB-SUB-OUT output interface.
 *
 * Emits `save` with the built payload; the parent owns API invocation and
 * reloads the list afterwards. Keeps the dialog reusable across create/edit.
 */
const props = defineProps<{
  modelValue: boolean;
  mode: 'create' | 'edit';
  record: OutputInterfaceResponse | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [payload: OutputInterfaceRequest];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isCreate = computed(() => props.mode === 'create');
const dialogTitle = computed(() => (isCreate.value ? '新建输出接口' : '编辑输出接口'));

interface FormModel {
  interfaceName: string;
  interfaceUrl: string;
  businessTypeId: string | null;
  authType: InterfaceAuthType | null;
  timeoutSeconds: number;
  retryCount: number;
}

const form = reactive<FormModel>({
  interfaceName: '',
  interfaceUrl: '',
  businessTypeId: null,
  authType: 'NONE',
  timeoutSeconds: 30,
  retryCount: 3,
});

const rules: FormRules<FormModel> = {
  interfaceName: [
    { required: true, message: '请输入接口名称', trigger: 'blur' },
    { min: 1, max: 30, message: '接口名称长度 1-30 字符', trigger: 'blur' },
  ],
  interfaceUrl: [
    { required: true, message: '请输入接口地址', trigger: 'blur' },
    { pattern: /^https?:\/\//, message: '接口地址需以 http:// 或 https:// 开头', trigger: 'blur' },
  ],
  authType: [
    { required: true, message: '请选择鉴权类型', trigger: 'change' },
  ],
  timeoutSeconds: [
    { type: 'number', min: 1, max: 300, message: '超时秒数 1-300', trigger: 'blur' },
  ],
  retryCount: [
    { type: 'number', min: 0, max: 10, message: '重试次数 0-10', trigger: 'blur' },
  ],
};

watch(() => [props.modelValue, props.record] as const, ([visible]) => {
  if (!visible) return;
  if (props.record) {
    Object.assign(form, {
      interfaceName: props.record.interfaceName,
      interfaceUrl: props.record.interfaceUrl,
      businessTypeId: props.record.businessTypeId ?? null,
      authType: props.record.authType,
      timeoutSeconds: props.record.timeoutSeconds,
      retryCount: props.record.retryCount,
    });
  } else {
    Object.assign(form, {
      interfaceName: '',
      interfaceUrl: '',
      businessTypeId: null,
      authType: 'NONE' as InterfaceAuthType,
      timeoutSeconds: 30,
      retryCount: 3,
    });
  }
}, { immediate: true });

async function onSave() {
  if (formRef.value) {
    // Element Plus FormInstance.validate() rejects with invalidFields when
    // any rule fails; inline errors auto-render via rules.message. Bail
    // silently on reject — EP already surfaces the errors under each field.
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) return;
  }
  // authType is non-null here: the required rule rejects validate() otherwise.
  submitting.value = true;
  try {
    const payload: OutputInterfaceRequest = {
      interfaceName: form.interfaceName,
      interfaceUrl: form.interfaceUrl,
      businessTypeId: form.businessTypeId || null,
      authType: form.authType!,
      timeoutSeconds: form.timeoutSeconds,
      retryCount: form.retryCount,
    };
    emit('save', payload);
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}

defineExpose({ onSave, rules, formRef });
</script>
