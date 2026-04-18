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
        label="数据源名称"
        prop="sourceName"
      >
        <el-input
          v-model="form.sourceName"
          placeholder="1-30 个字符"
          maxlength="30"
        />
      </el-form-item>
      <el-form-item
        label="Logo 路径"
        prop="logoPath"
      >
        <el-input
          v-model="form.logoPath"
          placeholder="可选"
        />
      </el-form-item>
      <el-form-item
        label="联系地址"
        prop="contactAddress"
      >
        <el-input
          v-model="form.contactAddress"
          placeholder="1-50 个字符"
          maxlength="50"
        />
      </el-form-item>
      <el-form-item
        label="联系电话"
        prop="contactPhone"
      >
        <el-input
          v-model="form.contactPhone"
          placeholder="1-11 位数字"
          :maxlength="11"
        />
      </el-form-item>
      <el-form-item
        label="推送启用"
        prop="pushEnabled"
      >
        <el-switch v-model="form.pushEnabled" />
      </el-form-item>
      <el-form-item
        label="内容类型"
        prop="contentType"
      >
        <el-input
          v-model="form.contentType"
          placeholder="可选，例如 application/json"
        />
      </el-form-item>
      <el-form-item
        label="客户端 ID"
        prop="clientId"
      >
        <el-input
          v-model="form.clientId"
          placeholder="可选"
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
import type { DataSourceRequest, DataSourceResponse } from '../api/sub-data-source-api';

/**
 * Edit/create dialog for FR-WEB-SUB-SRC data source (PRD §5.5.3).
 *
 * Emits `save` with the built payload; the parent owns API invocation and
 * reloads the list afterwards. Mirrors OutputInterfaceEditDialog pattern
 * (shared test approach: stub formRef.validate rejection for negative-path
 * behavior tests, because JSDOM does not reliably register fields teleported
 * under el-dialog for real rule-driven validate() calls).
 */
const props = defineProps<{
  modelValue: boolean;
  mode: 'create' | 'edit';
  record: DataSourceResponse | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [payload: DataSourceRequest];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isCreate = computed(() => props.mode === 'create');
const dialogTitle = computed(() => (isCreate.value ? '新建数据源' : '编辑数据源'));

interface FormModel {
  sourceName: string;
  logoPath: string;
  contactAddress: string;
  contactPhone: string;
  pushEnabled: boolean;
  contentType: string;
  clientId: string;
}

const form = reactive<FormModel>({
  sourceName: '',
  logoPath: '',
  contactAddress: '',
  contactPhone: '',
  pushEnabled: false,
  contentType: '',
  clientId: '',
});

const rules: FormRules<FormModel> = {
  sourceName: [
    { required: true, message: '请输入数据源名称', trigger: 'blur' },
    { min: 1, max: 30, message: '数据源名称长度 1-30 字符', trigger: 'blur' },
  ],
  contactAddress: [
    { required: true, message: '请输入联系地址', trigger: 'blur' },
    { min: 1, max: 50, message: '联系地址长度 1-50 字符', trigger: 'blur' },
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^\d{1,11}$/, message: '联系电话为 1-11 位数字', trigger: 'blur' },
  ],
};

watch(() => [props.modelValue, props.record] as const, ([visible]) => {
  if (!visible) return;
  if (props.record) {
    Object.assign(form, {
      sourceName: props.record.sourceName,
      logoPath: props.record.logoPath ?? '',
      contactAddress: props.record.contactAddress,
      contactPhone: props.record.contactPhone,
      pushEnabled: props.record.pushEnabled,
      contentType: props.record.contentType ?? '',
      clientId: props.record.clientId ?? '',
    });
  } else {
    Object.assign(form, {
      sourceName: '',
      logoPath: '',
      contactAddress: '',
      contactPhone: '',
      pushEnabled: false,
      contentType: '',
      clientId: '',
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
  submitting.value = true;
  try {
    const payload: DataSourceRequest = {
      sourceName: form.sourceName,
      logoPath: form.logoPath || null,
      contactAddress: form.contactAddress,
      contactPhone: form.contactPhone,
      pushEnabled: form.pushEnabled,
      contentType: form.contentType || null,
      clientId: form.clientId || null,
    };
    emit('save', payload);
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}

defineExpose({ onSave, rules, formRef, form });
</script>
