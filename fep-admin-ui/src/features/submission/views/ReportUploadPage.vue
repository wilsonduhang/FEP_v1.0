<!-- src/features/submission/views/ReportUploadPage.vue -->
<template>
  <div class="report-upload-page">
    <div class="header">
      <el-page-header title="报送管理" content="手动报文上传" class="page-header" />
      <MockBadge size="small">文件解析 P1 就绪后启用</MockBadge>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="upload-form"
    >
      <el-form-item label="报文类型" prop="messageType">
        <el-select
          v-model="form.messageType"
          placeholder="请选择报文类型"
          style="width: 320px"
          filterable
        >
          <el-option
            v-for="opt in messageTypeOptions"
            :key="opt.code"
            :label="`${opt.code} - ${opt.name}`"
            :value="opt.code"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="上传文件" prop="fileName">
        <el-upload
          :before-upload="beforeFileUpload"
          :limit="1"
          :auto-upload="false"
          accept=".xls,.xlsx,.csv,.xml"
          data-test="file-upload"
        >
          <el-button type="primary">选择文件</el-button>
          <template #tip>
            <div class="upload-tip">支持 xls/xlsx/csv/xml；文件不会真实上传（P1 后启用）</div>
          </template>
        </el-upload>
        <span v-if="form.fileName" class="selected-file">已选：{{ form.fileName }}</span>
      </el-form-item>

      <el-form-item label="报文名称" prop="messageName">
        <el-input v-model="form.messageName" placeholder="可从文件名自动填充" style="width: 320px" />
      </el-form-item>

      <el-form-item label="数据条数" prop="dataCount">
        <el-input-number v-model="form.dataCount" :min="1" :max="999999" />
      </el-form-item>

      <el-form-item label="业务类型 ID">
        <el-input v-model="form.businessTypeId" placeholder="可选" style="width: 320px" />
      </el-form-item>

      <el-form-item label="录入人">
        <el-input v-model="form.entryBy" placeholder="可选" style="width: 320px" />
      </el-form-item>

      <el-form-item label="附件说明">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          placeholder="可选备注，仅前端保存"
          style="width: 520px"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          data-test="submit-upload"
          :disabled="!canSubmit"
          :loading="submitting"
          @click="onSubmit"
        >
          提交上传
        </el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { MockBadge } from '@/shared/components';
import { subReportApi } from '../api/sub-report-api';
import { bizMessageDefinitionApi } from '@/features/biz-data/api/biz-message-definition-api';

interface UploadForm {
  messageType: string;
  messageName: string;
  businessTypeId: string;
  dataCount: number;
  entryBy: string;
  fileName: string;
  remark: string;
}

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive<UploadForm>({
  messageType: '',
  messageName: '',
  businessTypeId: '',
  dataCount: 1,
  entryBy: '',
  fileName: '',
  remark: '',
});

const rules = reactive<FormRules<UploadForm>>({
  messageType: [{ required: true, message: '请选择报文类型', trigger: 'change' }],
  messageName: [{ required: true, message: '请输入报文名称', trigger: 'blur' }],
  fileName: [{ required: true, message: '请选择文件', trigger: 'change' }],
  dataCount: [{ required: true, type: 'number', min: 1, message: '数据条数至少为 1', trigger: 'change' }],
});

const messageTypeOptions = ref<Array<{ code: string; name: string }>>([]);
const loadingOptions = ref(false);

async function loadMessageTypeOptions(): Promise<void> {
  loadingOptions.value = true;
  try {
    const page = await bizMessageDefinitionApi.search({ pageNum: 1, pageSize: 100 });
    messageTypeOptions.value = page.records.map((r) => ({
      code: r.messageCode,
      name: r.messageName,
    }));
  } catch (err: any) {
    ElMessage.error(err?.message ?? '报文类型加载失败');
  } finally {
    loadingOptions.value = false;
  }
}

const canSubmit = computed(() =>
  form.messageType && form.messageName && form.fileName && form.dataCount > 0,
);

function beforeFileUpload(file: File): boolean {
  const allowed = ['xls', 'xlsx', 'csv', 'xml'];
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (!ext || !allowed.includes(ext)) {
    ElMessage.error(
      `不支持的扩展名 .${ext ?? ''}，仅接受 ${allowed.map((e) => `.${e}`).join(' / ')}`,
    );
    return false;
  }
  form.fileName = file.name;
  const nameSansExt = file.name.replace(/\.[^.]+$/, '');
  if (!form.messageName) {
    form.messageName = nameSansExt;
  }
  return false;
}

async function onSubmit(): Promise<void> {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    await subReportApi.uploadRecord({
      messageType: form.messageType,
      messageName: form.messageName,
      businessTypeId: form.businessTypeId || undefined,
      dataCount: form.dataCount,
      entryBy: form.entryBy || undefined,
    });
    ElMessage.success('已上传');
    onReset();
  } catch (err: any) {
    ElMessage.error(err?.message ?? '上传失败');
  } finally {
    submitting.value = false;
  }
}

function onReset(): void {
  formRef.value?.resetFields();
  Object.assign(form, {
    messageType: '',
    messageName: '',
    businessTypeId: '',
    dataCount: 1,
    entryBy: '',
    fileName: '',
    remark: '',
  });
}

onMounted(loadMessageTypeOptions);

defineExpose({ form, beforeFileUpload, onSubmit, onReset, loadMessageTypeOptions });
</script>

<style scoped>
.report-upload-page { padding: 24px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header { flex: 1; }
.upload-form { max-width: 720px; }
.selected-file { margin-left: 12px; color: #606266; font-size: 13px; }
.upload-tip { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
