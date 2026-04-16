<template>
  <el-dialog
    :model-value="modelValue"
    title="新建查询任务"
    width="520px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item
        label="企业 ID"
        prop="enterpriseId"
      >
        <el-input
          v-model="form.enterpriseId"
          placeholder="发起查询的企业 ID"
        />
      </el-form-item>
      <el-form-item
        label="查询类型"
        prop="queryType"
      >
        <el-radio-group v-model="form.queryType">
          <el-radio value="REALTIME">
            实时（1001→2001）
          </el-radio>
          <el-radio value="BATCH">
            批量（1103→2103）
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        label="被查询 USCI"
        prop="usci"
      >
        <el-input
          v-model="form.usci"
          placeholder="18 位统一社会信用代码"
          maxlength="18"
        />
      </el-form-item>
      <el-form-item
        label="企业名称"
        prop="queryTargetName"
      >
        <el-input
          v-model="form.queryTargetName"
          placeholder="可选"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item
        v-if="form.queryType === 'BATCH'"
        label="批量文件路径"
        prop="batchFilePath"
      >
        <el-input
          v-model="form.batchFilePath"
          placeholder="服务器端文件路径占位（P7.2b 接入真实上传）"
          maxlength="500"
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
        @click="onSubmit"
      >
        创建
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { entQueryTaskApi, type QueryTaskCreateRequest } from '../api/ent-query-task-api';

const props = defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'created': [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive<QueryTaskCreateRequest>({
  enterpriseId: '', queryType: 'REALTIME', usci: '', queryTargetName: '', batchFilePath: '',
});

const rules: FormRules<QueryTaskCreateRequest> = {
  enterpriseId: [{ required: true, message: '请输入企业 ID', trigger: 'blur' }],
  queryType: [{ required: true, message: '请选择查询类型', trigger: 'change' }],
  usci: [
    { required: true, message: '请输入 USCI', trigger: 'blur' },
    { pattern: /^[0-9A-Z]{18}$/, message: 'USCI 必须为 18 位大写字母或数字', trigger: 'blur' },
  ],
  queryTargetName: [{ max: 200, message: '最多 200 字符', trigger: 'blur' }],
  batchFilePath: [{ max: 500, message: '最多 500 字符', trigger: 'blur' }],
};

watch(() => props.modelValue, (v) => {
  if (v) {
    Object.assign(form, { enterpriseId: '', queryType: 'REALTIME', usci: '', queryTargetName: '', batchFilePath: '' });
  }
});

async function onSubmit() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await entQueryTaskApi.create({
      enterpriseId: form.enterpriseId,
      queryType: form.queryType,
      usci: form.usci,
      queryTargetName: form.queryTargetName || undefined,
      batchFilePath: form.queryType === 'BATCH' ? form.batchFilePath || undefined : undefined,
    });
    emit('created');
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}
</script>
