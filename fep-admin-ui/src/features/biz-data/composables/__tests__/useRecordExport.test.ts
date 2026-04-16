import { describe, expect, it, vi, beforeEach } from 'vitest';
import { useRecordExport } from '../useRecordExport';
import { bizMessageRecordApi, type RecordSearchParams } from '../../api/biz-message-record-api';

vi.mock('../../api/biz-message-record-api');
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() },
}));

const params: RecordSearchParams = { pageNum: 1, pageSize: 20 };

describe('useRecordExport', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns task id on success', async () => {
    vi.mocked(bizMessageRecordApi.exportRecords).mockResolvedValue('TASK-001');
    const { triggerExport } = useRecordExport();
    const result = await triggerExport(params);
    expect(result).toBe('TASK-001');
    expect(bizMessageRecordApi.exportRecords).toHaveBeenCalledWith(params);
  });

  it('returns null on failure', async () => {
    vi.mocked(bizMessageRecordApi.exportRecords).mockRejectedValue(new Error('fail'));
    const { triggerExport } = useRecordExport();
    const result = await triggerExport(params);
    expect(result).toBeNull();
  });

  it('toggles exporting flag', async () => {
    let resolvePromise: (v: string) => void;
    vi.mocked(bizMessageRecordApi.exportRecords).mockImplementation(
      () =>
        new Promise<string>((resolve) => {
          resolvePromise = resolve;
        }),
    );
    const { exporting, triggerExport } = useRecordExport();
    expect(exporting.value).toBe(false);
    const promise = triggerExport(params);
    expect(exporting.value).toBe(true);
    resolvePromise!('TASK-002');
    await promise;
    expect(exporting.value).toBe(false);
  });
});
