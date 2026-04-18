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
        label="场景名称"
        prop="sceneName"
      >
        <el-input
          v-model="form.sceneName"
          placeholder="3-30 个字符"
          maxlength="30"
        />
      </el-form-item>
      <el-form-item
        label="业务类型"
        prop="businessTypeId"
      >
        <el-select
          v-model="form.businessTypeId"
          placeholder="请选择"
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
        label="推送方式"
        prop="pushMethod"
      >
        <el-select
          v-model="form.pushMethod"
          placeholder="请选择"
          style="width: 100%"
        >
          <el-option
            label="自动"
            value="AUTO"
          />
          <el-option
            label="手动"
            value="MANUAL"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="导入模板路径"
        prop="importTemplatePath"
        data-test="import-template-path"
      >
        <el-input
          v-model="form.importTemplatePath"
          :disabled="form.pushMethod === 'AUTO'"
          placeholder="MANUAL 模式下填写模板文件路径"
        />
      </el-form-item>
      <el-form-item
        label="请求地址"
        prop="requestUrl"
      >
        <el-input
          v-model="form.requestUrl"
          placeholder="http:// 或 https:// 开头"
        />
      </el-form-item>
      <el-form-item
        label="排序"
        prop="sortOrder"
      >
        <el-input-number
          v-model="form.sortOrder"
          :min="0"
          :precision="0"
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
  BusinessSceneRequest,
  BusinessSceneResponse,
  ScenePushMethod,
} from '../api/sub-business-scene-api';
import { DEFAULT_BIZ_TYPE_OPTIONS } from '@/features/biz-data/constants/biz-type-options';

/**
 * Edit/create dialog for FR-WEB-SUB-SCENE business scene (PRD §5.5.4).
 *
 * Contract-baseline notes baked into the form:
 *  - `pushMethod` is the 2-value enum AUTO|MANUAL (no SCHEDULE / no cron).
 *  - `importTemplatePath` is a plain string file-path reference; the input is
 *    disabled (and auto-cleared) in AUTO mode, required in MANUAL mode.
 *
 * Testing strategy mirrors DataSourceEditDialog / OutputInterfaceEditDialog:
 *  - Prefer behavior-level checks (DOM disabled state, real rule.validator,
 *    stubbed formRef.validate rejection) over rule-shape assertions.
 *  - `defineExpose({ onSave, rules, formRef, form })` lets the spec poke at
 *    internals without resorting to teleported-dialog DOM quirks in JSDOM.
 */
const props = defineProps<{
  modelValue: boolean;
  mode: 'create' | 'edit';
  record: BusinessSceneResponse | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  save: [payload: BusinessSceneRequest];
}>();

const formRef = ref<FormInstance>();
const submitting = ref(false);

const isCreate = computed(() => props.mode === 'create');
const dialogTitle = computed(() => (isCreate.value ? '新建业务场景' : '编辑业务场景'));

interface FormModel {
  sceneName: string;
  businessTypeId: string;
  pushMethod: ScenePushMethod;
  importTemplatePath: string;
  requestUrl: string;
  sortOrder: number;
}

const form = reactive<FormModel>({
  sceneName: '',
  businessTypeId: '',
  pushMethod: 'AUTO',
  importTemplatePath: '',
  requestUrl: '',
  sortOrder: 0,
});

const rules: FormRules<FormModel> = {
  sceneName: [
    { required: true, message: '场景名称必填', trigger: 'blur' },
    { min: 3, max: 30, message: '长度 3-30 字符', trigger: 'blur' },
  ],
  businessTypeId: [
    { required: true, message: '业务类型必填', trigger: 'change' },
  ],
  pushMethod: [
    { required: true, message: '推送方式必填', trigger: 'change' },
  ],
  importTemplatePath: [
    {
      // MANUAL 模式下模板路径必填；AUTO 模式下该字段忽略。
      validator: (_rule, value: string, callback: (err?: Error) => void) => {
        if (form.pushMethod === 'MANUAL' && !value) {
          callback(new Error('MANUAL 模式需填模板路径'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
  requestUrl: [
    { required: true, message: '请求地址必填', trigger: 'blur' },
    { pattern: /^https?:\/\//, message: '必须是 http(s) URL', trigger: 'blur' },
  ],
  sortOrder: [
    { required: true, message: '排序数值必填', trigger: 'change' },
    { type: 'integer', message: '必须是整数', trigger: 'change' },
  ],
};

// Auto-clear importTemplatePath when switching to AUTO (contract: AC #5).
watch(
  () => form.pushMethod,
  (m) => {
    if (m === 'AUTO') {
      form.importTemplatePath = '';
    }
  },
);

watch(() => [props.modelValue, props.record] as const, ([visible]) => {
  if (!visible) return;
  if (props.record) {
    Object.assign(form, {
      sceneName: props.record.sceneName,
      businessTypeId: props.record.businessTypeId,
      pushMethod: props.record.pushMethod,
      importTemplatePath: props.record.importTemplatePath ?? '',
      requestUrl: props.record.requestUrl,
      sortOrder: props.record.sortOrder,
    });
  } else {
    Object.assign(form, {
      sceneName: '',
      businessTypeId: '',
      pushMethod: 'AUTO' as ScenePushMethod,
      importTemplatePath: '',
      requestUrl: '',
      sortOrder: 0,
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
    const payload: BusinessSceneRequest = {
      sceneName: form.sceneName,
      businessTypeId: form.businessTypeId,
      pushMethod: form.pushMethod,
      importTemplatePath:
        form.pushMethod === 'MANUAL' ? form.importTemplatePath : null,
      requestUrl: form.requestUrl,
      sortOrder: form.sortOrder,
    };
    emit('save', payload);
    emit('update:modelValue', false);
  } finally {
    submitting.value = false;
  }
}

defineExpose({ onSave, rules, formRef, form });
</script>
