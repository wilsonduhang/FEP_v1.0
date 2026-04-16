<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="560px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :disabled="isReadonly"
      label-width="120px"
    >
      <el-form-item
        label="企业 ID"
        prop="enterpriseId"
      >
        <el-input
          v-model="form.enterpriseId"
          placeholder="发起授权的企业 ID"
        />
      </el-form-item>
      <el-form-item
        label="授权类型"
        prop="authType"
      >
        <el-radio-group v-model="form.authType">
          <el-radio value="PAPER">
            纸质
          </el-radio>
          <el-radio value="ELECTRONIC">
            电子
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        label="被授权 USCI"
        prop="authorizedUsci"
      >
        <el-input
          v-model="form.authorizedUsci"
          placeholder="18 位统一社会信用代码"
          maxlength="18"
        />
      </el-form-item>
      <el-form-item
        label="被授权名称"
        prop="authorizedName"
      >
        <el-input
          v-model="form.authorizedName"
          placeholder="可选"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item
        label="授权范围"
        prop="authScope"
      >
        <el-input
          v-model="form.authScope"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="可选"
        />
      </el-form-item>
      <el-form-item
        v-if="form.authType === 'ELECTRONIC'"
        label="文件路径"
        prop="filePath"
      >
        <el-input
          v-model="form.filePath"
          placeholder="服务器端文件路径占位（P7.2b 接入真实上传）"
          maxlength="500"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ isReadonly ? '关闭' : '取消' }}
      </el-button>
      <el-button
        v-if="!isReadonly"
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
import { ElMessage } from 'element-plus';
import {
  entAuthLetterApi,
  type AuthLetterCreateRequest,
  type AuthLetterResponse,
} from '../api/ent-auth-letter-api';

const props = defineProps<{
  modelValue: boolean;
  letter: AuthLetterResponse | null;
}>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'saved': [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isCreate = computed(() => props.letter === null);
const isReadonly = computed(
  () => props.letter !== null && props.letter.letterStatus !== 'DRAFT',
);
const dialogTitle = computed(() => {
  if (isCreate.value) return '新建授权书';
  if (isReadonly.value) return '查看授权书';
  return '编辑授权书';
});

interface FormModel {
  enterpriseId: string;
  authType: 'PAPER' | 'ELECTRONIC';
  authorizedUsci: string;
  authorizedName: string;
  authScope: string;
  filePath: string;
}

const form = reactive<FormModel>({
  enterpriseId: '',
  authType: 'PAPER',
  authorizedUsci: '',
  authorizedName: '',
  authScope: '',
  filePath: '',
});

const rules: FormRules<FormModel> = {
  enterpriseId: [{ required: true, message: '请输入企业 ID', trigger: 'blur' }],
  authType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  authorizedUsci: [
    { required: true, message: '请输入被授权 USCI', trigger: 'blur' },
    { pattern: /^[0-9A-Z]{18}$/, message: 'USCI 必须为 18 位大写字母或数字', trigger: 'blur' },
  ],
  authorizedName: [{ max: 200, message: '最多 200 字符', trigger: 'blur' }],
  authScope: [{ max: 500, message: '最多 500 字符', trigger: 'blur' }],
  filePath: [{ max: 500, message: '最多 500 字符', trigger: 'blur' }],
};

watch(() => props.modelValue, (v) => {
  if (v) {
    if (props.letter) {
      Object.assign(form, {
        enterpriseId: props.letter.enterpriseId,
        authType: props.letter.authType,
        authorizedUsci: props.letter.authorizedUsci,
        authorizedName: props.letter.authorizedName ?? '',
        authScope: props.letter.authScope ?? '',
        filePath: props.letter.filePath ?? '',
      });
    } else {
      Object.assign(form, {
        enterpriseId: '',
        authType: 'PAPER',
        authorizedUsci: '',
        authorizedName: '',
        authScope: '',
        filePath: '',
      });
    }
  }
}, { immediate: true });

async function onSave() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload: AuthLetterCreateRequest = {
      enterpriseId: form.enterpriseId,
      authType: form.authType,
      authorizedUsci: form.authorizedUsci,
      authorizedName: form.authorizedName || undefined,
      authScope: form.authScope || undefined,
      filePath: form.authType === 'ELECTRONIC' ? form.filePath || undefined : undefined,
    };
    if (isCreate.value) {
      await entAuthLetterApi.create(payload);
      ElMessage.success('已创建');
    } else {
      await entAuthLetterApi.update(props.letter!.letterId, payload);
      ElMessage.success('已更新');
    }
    emit('saved');
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}
</script>
