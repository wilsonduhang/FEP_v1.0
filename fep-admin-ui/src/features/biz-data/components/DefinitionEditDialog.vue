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
        label="报文编码"
        prop="messageCode"
      >
        <el-input
          v-model="form.messageCode"
          :disabled="!isCreate"
          placeholder="1-5 位数字编码"
        />
      </el-form-item>
      <el-form-item
        label="报文名称"
        prop="messageName"
      >
        <el-input
          v-model="form.messageName"
          placeholder="2-200 个字符"
          maxlength="200"
        />
      </el-form-item>
      <el-form-item
        label="方向"
        prop="direction"
      >
        <el-radio-group v-model="form.direction">
          <el-radio value="OUTBOUND">
            出站
          </el-radio>
          <el-radio value="INBOUND">
            入站
          </el-radio>
          <el-radio value="BIDIRECTIONAL">
            双向
          </el-radio>
        </el-radio-group>
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
        label="字段数"
        prop="fieldCount"
      >
        <el-input-number
          v-model="form.fieldCount"
          :min="0"
        />
      </el-form-item>
      <el-form-item
        label="字段摘要"
        prop="fieldSummary"
      >
        <el-input
          v-model="form.fieldSummary"
          type="textarea"
          :rows="3"
          placeholder="可选"
        />
      </el-form-item>
      <el-form-item
        label="示例 XML"
        prop="sampleXml"
      >
        <el-input
          v-model="form.sampleXml"
          type="textarea"
          :rows="5"
          placeholder="可选"
          style="font-family: monospace"
        />
      </el-form-item>
      <el-form-item
        label="排序"
        prop="sortOrder"
      >
        <el-input-number
          v-model="form.sortOrder"
          :min="0"
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
import { ElMessage } from 'element-plus';
import {
  bizMessageDefinitionApi,
  type DefinitionCreateRequest,
  type DefinitionResponse,
  type MessageDirection,
} from '../api/biz-message-definition-api';
import { DEFAULT_BIZ_TYPE_OPTIONS } from '../constants/biz-type-options';

const props = defineProps<{
  modelValue: boolean;
  definition: DefinitionResponse | null;
}>();
const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'saved': [];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isCreate = computed(() => props.definition === null);
const dialogTitle = computed(() => (isCreate.value ? '新建报文定义' : '编辑报文定义'));

interface FormModel {
  messageCode: string;
  messageName: string;
  direction: MessageDirection;
  businessTypeId: string;
  fieldCount: number;
  fieldSummary: string;
  sampleXml: string;
  sortOrder: number;
}

const form = reactive<FormModel>({
  messageCode: '',
  messageName: '',
  direction: 'OUTBOUND',
  businessTypeId: '',
  fieldCount: 0,
  fieldSummary: '',
  sampleXml: '',
  sortOrder: 0,
});

const rules: FormRules<FormModel> = {
  messageCode: [
    { required: true, message: '请输入报文编码', trigger: 'blur' },
    { pattern: /^\d{1,5}$/, message: '报文编码为 1-5 位数字', trigger: 'blur' },
  ],
  messageName: [
    { required: true, message: '请输入报文名称', trigger: 'blur' },
    { min: 2, max: 200, message: '报文名称需 2-200 字符', trigger: 'blur' },
  ],
  direction: [
    { required: true, message: '请选择方向', trigger: 'change' },
  ],
};

watch(() => props.modelValue, (v) => {
  if (v) {
    if (props.definition) {
      Object.assign(form, {
        messageCode: props.definition.messageCode,
        messageName: props.definition.messageName,
        direction: props.definition.direction,
        businessTypeId: props.definition.businessTypeId ?? '',
        fieldCount: props.definition.fieldCount,
        fieldSummary: props.definition.fieldSummary ?? '',
        sampleXml: props.definition.sampleXml ?? '',
        sortOrder: props.definition.sortOrder,
      });
    } else {
      Object.assign(form, {
        messageCode: '',
        messageName: '',
        direction: 'OUTBOUND',
        businessTypeId: '',
        fieldCount: 0,
        fieldSummary: '',
        sampleXml: '',
        sortOrder: 0,
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
    const payload: DefinitionCreateRequest = {
      messageCode: form.messageCode,
      messageName: form.messageName,
      direction: form.direction,
      businessTypeId: form.businessTypeId || undefined,
      fieldCount: form.fieldCount,
      fieldSummary: form.fieldSummary || undefined,
      sampleXml: form.sampleXml || undefined,
      sortOrder: form.sortOrder,
    };
    if (isCreate.value) {
      await bizMessageDefinitionApi.create(payload);
      ElMessage.success('已创建');
    } else {
      await bizMessageDefinitionApi.update(props.definition!.definitionId, payload);
      ElMessage.success('已更新');
    }
    emit('saved');
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}
</script>
