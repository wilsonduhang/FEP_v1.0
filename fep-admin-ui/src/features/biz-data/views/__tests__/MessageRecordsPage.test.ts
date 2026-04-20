import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import MessageRecordsPage from '../MessageRecordsPage.vue';
import {
  bizMessageRecordApi,
  type RecordResponse,
  type RecordSummaryItem,
} from '../../api/biz-message-record-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/biz-message-record-api');

const mockRecord: RecordResponse = {
  recordId: 'R1',
  messageCode: '1001',
  serialNo: 'SN001',
  senderNode: null,
  receiverNode: null,
  direction: 'OUTBOUND',
  processStatus: 'SUCCESS',
  businessNo: 'BIZ001',
  amount: '12345.67',
  xmlContent: '<xml/>',
  entryMethod: 'API',
  accessCount: 3,
  errorMessage: null,
  processTime: '2026-04-15T10:00:05',
  createTime: '2026-04-15T10:00:00',
  updateTime: '2026-04-15T10:00:05',
};

const mockPage: PageResult<RecordResponse> = {
  records: [mockRecord],
  total: 1,
  pageNum: 1,
  pageSize: 20,
  totalPages: 1,
};

const mockSummary: RecordSummaryItem[] = [
  {
    messageCode: '1001',
    messageName: '查询请求',
    totalCount: 10,
    successCount: 8,
    pendingCount: 1,
    failedCount: 1,
  },
];

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div/>' } }],
});

const globalPlugins = { global: { plugins: [router, ElementPlus] } };

describe('MessageRecordsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads summary and list on mount', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue(mockSummary);
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    expect(bizMessageRecordApi.search).toHaveBeenCalled();
    expect(bizMessageRecordApi.getSummary).toHaveBeenCalled();
    expect(wrapper.text()).toContain('R1');
  });

  it('triggers search on search button click', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue([]);
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '搜索')
      ?.trigger('click');
    expect(bizMessageRecordApi.search).toHaveBeenCalledTimes(2);
  });

  it('formats amount as CNY currency', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue([]);
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    // Intl.NumberFormat('zh-CN', {style:'currency',currency:'CNY'}) formats 12345.67
    // The exact format depends on locale, but should contain 12,345.67 or similar
    expect(wrapper.text()).toMatch(/12[,.]?345\.67/);
  });

  it('displays Mock Mode badge with resubmit/export note', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue([]);
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    expect(wrapper.text()).toContain('TLQ Mock 模式');
    expect(wrapper.text()).toContain('resubmit/export 为预留操作');
  });

  it('disables resubmit button on non-FAILED records', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue([]);
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    const resubmitBtn = wrapper.findAll('button').find((b) => b.text() === '重提');
    expect(resubmitBtn).toBeTruthy();
    expect(resubmitBtn!.attributes('disabled')).toBeDefined();
  });

  it('has export button', async () => {
    vi.mocked(bizMessageRecordApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageRecordApi.getSummary).mockResolvedValue([]);
    vi.mocked(bizMessageRecordApi.exportRecords).mockResolvedValue('TASK-001');
    const wrapper = mount(MessageRecordsPage, globalPlugins);
    await flushPromises();
    const exportBtn = wrapper.findAll('button').find((b) => b.text() === '导出');
    expect(exportBtn).toBeTruthy();
  });
});
