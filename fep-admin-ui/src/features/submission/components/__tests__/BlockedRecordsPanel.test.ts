// src/features/submission/components/__tests__/BlockedRecordsPanel.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import BlockedRecordsPanel from '../BlockedRecordsPanel.vue';
import { subReportApi } from '../../api/sub-report-api';
import { makeRecord } from '../../__tests__/fixtures/record';

vi.mock('../../api/sub-report-api', () => ({
  subReportApi: { getBlocked: vi.fn() },
}));
const mockGetBlocked = vi.mocked(subReportApi.getBlocked);

// Mock ElMessage so error path is observable and does not render.
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus');
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

const globalOpts = { global: { plugins: [ElementPlus] } };

beforeEach(() => {
  vi.clearAllMocks();
});

describe('BlockedRecordsPanel', () => {
  it('calls getBlocked with (1, 10) on mount and populates page', async () => {
    mockGetBlocked.mockResolvedValue({
      records: [makeRecord({ recordId: 'R1', pushStatus: 'FAILED' })],
      total: 1,
      pageNum: 1,
      pageSize: 10,
      totalPages: 1,
    });
    const wrapper = mount(BlockedRecordsPanel, globalOpts);
    await flushPromises();
    expect(mockGetBlocked).toHaveBeenCalledWith(1, 10);
    const vm = wrapper.vm as any;
    expect(vm.page.total).toBe(1);
    expect(vm.page.records).toHaveLength(1);
  });

  it('failedRecords computed filters to FAILED only (PUSHING excluded)', async () => {
    mockGetBlocked.mockResolvedValue({
      records: [
        makeRecord({ recordId: 'R1', pushStatus: 'FAILED' }),
        makeRecord({ recordId: 'R2', pushStatus: 'PUSHING' }),
        makeRecord({ recordId: 'R3', pushStatus: 'FAILED' }),
      ],
      total: 3,
      pageNum: 1,
      pageSize: 10,
      totalPages: 1,
    });
    const wrapper = mount(BlockedRecordsPanel, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    expect(vm.failedRecords).toHaveLength(2);
    expect(vm.failedRecords.map((r: any) => r.recordId)).toEqual(['R1', 'R3']);
  });

  it('renders FAILED-only semantic alert explaining PUSHING exclusion', async () => {
    mockGetBlocked.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    const wrapper = mount(BlockedRecordsPanel, globalOpts);
    await flushPromises();
    const alert = wrapper.find('[data-test="blocked-semantics-alert"]');
    expect(alert.exists()).toBe(true);
    expect(alert.text()).toContain('仅展示推送失败记录（FAILED）');
    expect(alert.text()).toContain('PUSHING 属正常流转');
  });

  it('surfaces error via ElMessage when getBlocked fails', async () => {
    // Node 24 CJK workaround: use `new Error(...)` for rejection.
    // Don't "normalize" this to `mockRejectedValue({code: '...', message: '...'})`.
    mockGetBlocked.mockRejectedValue(new Error('fetch fail'));
    mount(BlockedRecordsPanel, globalOpts);
    await flushPromises();
    expect(ElMessage.error).toHaveBeenCalledWith('fetch fail');
  });
});
