import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { bizMessageRecordApi, type RecordSearchParams } from '../api/biz-message-record-api';

/**
 * Composable for triggering record export tasks.
 * Returns a download task ID on success for the user to check in the download center.
 */
export function useRecordExport() {
  const exporting = ref(false);

  async function triggerExport(params: RecordSearchParams): Promise<string | null> {
    exporting.value = true;
    try {
      const downloadTaskId = await bizMessageRecordApi.exportRecords(params);
      ElMessage.success(`导出任务已创建，任务 ID: ${downloadTaskId}，请到下载中心查看`);
      return downloadTaskId;
    } catch {
      return null;
    } finally {
      exporting.value = false;
    }
  }

  return { exporting, triggerExport };
}
