<template>
  <el-dialog
    :model-value="modelValue"
    :title="isCreate ? '新建凭证' : '编辑凭证'"
    width="520px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="接口 ID" prop="interfaceId">
        <el-input v-model="form.interfaceId" :disabled="!isCreate" />
      </el-form-item>
      <el-form-item label="鉴权类型" prop="authType">
        <el-select v-model="form.authType" :disabled="!isCreate">
          <el-option label="TOKEN" value="TOKEN" />
          <el-option label="OAUTH2" value="OAUTH2" />
        </el-select>
      </el-form-item>

      <template v-if="form.authType === 'TOKEN'">
        <el-form-item label="Token" prop="token">
          <el-input
            v-model="form.token"
            type="password"
            show-password
            :placeholder="isCreate ? '请输入 Token' : '留空保留原密文'"
          />
        </el-form-item>
        <el-form-item label="Token Header">
          <el-input v-model="form.tokenHeader" placeholder="如 Authorization" />
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="Client ID" prop="oauthClientId">
          <el-input
            v-model="form.oauthClientId"
            type="password"
            show-password
            :placeholder="isCreate ? '请输入 Client ID' : '留空保留原密文'"
          />
        </el-form-item>
        <el-form-item label="Client Secret" prop="oauthClientSecret">
          <el-input
            v-model="form.oauthClientSecret"
            type="password"
            show-password
            :placeholder="isCreate ? '请输入 Client Secret' : '留空保留原密文'"
          />
        </el-form-item>
        <el-form-item label="Token Endpoint" prop="oauthTokenEndpoint">
          <el-input v-model="form.oauthTokenEndpoint" />
        </el-form-item>
        <el-form-item label="Scope">
          <el-input v-model="form.oauthScope" />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="onSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
  callbackCredentialApi,
  type CallbackAuthType,
  type CallbackCredentialCreateRequest,
  type CallbackCredentialResponse,
} from '../api/callbackCredential';

const props = defineProps<{ modelValue: boolean; record: CallbackCredentialResponse | null }>();
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>();

const formRef = ref<FormInstance>();
const submitting = ref(false);
const isCreate = computed(() => props.record === null);

interface FormModel {
  interfaceId: string;
  authType: CallbackAuthType;
  token: string;
  tokenHeader: string;
  oauthClientId: string;
  oauthClientSecret: string;
  oauthTokenEndpoint: string;
  oauthScope: string;
}

const form = reactive<FormModel>({
  interfaceId: '',
  authType: 'TOKEN',
  token: '',
  tokenHeader: '',
  oauthClientId: '',
  oauthClientSecret: '',
  oauthTokenEndpoint: '',
  oauthScope: '',
});

// R1 ISSUE-2: 创建时按 authType 条件必填密文（编辑时可选，留空=保留）。
// 用 computed rules 让校验随 authType / isCreate 动态切换。
const rules = computed<FormRules<FormModel>>(() => {
  const base: FormRules<FormModel> = {
    interfaceId: [{ required: true, message: '请输入接口 ID', trigger: 'blur' }],
    authType: [{ required: true, message: '请选择鉴权类型', trigger: 'change' }],
  };
  if (!isCreate.value) return base; // 编辑：密文留空保留，不强制
  if (form.authType === 'TOKEN') {
    base.token = [{ required: true, message: '请输入 Token', trigger: 'blur' }];
  } else {
    base.oauthClientId = [{ required: true, message: '请输入 Client ID', trigger: 'blur' }];
    base.oauthClientSecret = [
      { required: true, message: '请输入 Client Secret', trigger: 'blur' },
    ];
    base.oauthTokenEndpoint = [
      { required: true, message: '请输入 Token Endpoint', trigger: 'blur' },
    ];
  }
  return base;
});

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return;
    if (props.record) {
      Object.assign(form, {
        interfaceId: props.record.interfaceId,
        authType: props.record.authType,
        token: '',
        tokenHeader: props.record.tokenHeader ?? '',
        oauthClientId: '',
        oauthClientSecret: '',
        oauthTokenEndpoint: props.record.oauthTokenEndpoint ?? '',
        oauthScope: props.record.oauthScope ?? '',
      });
    } else {
      Object.assign(form, {
        interfaceId: '',
        authType: 'TOKEN',
        token: '',
        tokenHeader: '',
        oauthClientId: '',
        oauthClientSecret: '',
        oauthTokenEndpoint: '',
        oauthScope: '',
      });
    }
  },
  { immediate: true },
);

async function onSave() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (isCreate.value) {
      const payload: CallbackCredentialCreateRequest = {
        interfaceId: form.interfaceId,
        authType: form.authType,
        token: form.token || undefined,
        tokenHeader: form.tokenHeader || undefined,
        oauthClientId: form.oauthClientId || undefined,
        oauthClientSecret: form.oauthClientSecret || undefined,
        oauthTokenEndpoint: form.oauthTokenEndpoint || undefined,
        oauthScope: form.oauthScope || undefined,
      };
      await callbackCredentialApi.create(payload);
      ElMessage.success('已创建');
    } else {
      await callbackCredentialApi.update(form.interfaceId, {
        token: form.token || undefined,
        tokenHeader: form.tokenHeader || undefined,
        oauthClientId: form.oauthClientId || undefined,
        oauthClientSecret: form.oauthClientSecret || undefined,
        oauthTokenEndpoint: form.oauthTokenEndpoint || undefined,
        oauthScope: form.oauthScope || undefined,
      });
      ElMessage.success('已更新');
    }
    emit('saved');
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}
</script>
