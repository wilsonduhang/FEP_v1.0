// src/features/submission/views/__tests__/ReportRecordsPage.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import ReportRecordsPage from '../ReportRecordsPage.vue';
import { subReportApi } from '../../api/sub-report-api';

vi.mock('../../api/sub-report-api', () => ({
  subReportApi: { searchRecords: vi.fn() },
}));
const mockSearch = vi.mocked(subReportApi.searchRecords);

const mockPush = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}));

// Mock ElMessage so error-handling paths are observable and do not render.
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

describe('ReportRecordsPage', () => {
  it('renders 总报文数 stat with page.total', async () => {
    mockSearch.mockResolvedValue({
      records: [{ recordId: 'R1', messageType: '3001', messageName: 'Q', amount: '1234.56', dataCount: 10, entryMethod: 'API_CALL', createTime: '2026-04-18T10:00:00' } as any],
      total: 42, pageNum: 1, pageSize: 10, totalPages: 5,
    });
    const wrapper = mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    expect(wrapper.text()).toContain('42');
    expect(wrapper.text()).toContain('总报文数');
  });

  it('calls searchRecords on mount with default params', async () => {
    mockSearch.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });
    mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    expect(mockSearch).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 1, pageSize: 10 }));
  });

  it('formats amount with formatAmount helper', async () => {
    mockSearch.mockResolvedValue({
      records: [{ recordId: 'R1', messageType: '3001', messageName: 'Q', amount: '12345.67', dataCount: 10, entryMethod: 'API_CALL', createTime: '2026-04-18T10:00:00' } as any],
      total: 1, pageNum: 1, pageSize: 10, totalPages: 1,
    });
    const wrapper = mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    expect(wrapper.text()).toContain('12,345.67');
  });

  it('navigates to /report/view on row click', async () => {
    mockSearch.mockResolvedValue({
      records: [{ recordId: 'R1', messageType: '3001', messageName: 'Q', amount: null, dataCount: 1, entryMethod: 'API_CALL', createTime: '2026-04-18T10:00:00' } as any],
      total: 1, pageNum: 1, pageSize: 10, totalPages: 1,
    });
    const wrapper = mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    vm.onJumpView({ messageType: '3001' });
    expect(mockPush).toHaveBeenCalledWith({ path: '/report/view', query: { messageType: '3001' } });
  });

  it('opens drawer on view click', async () => {
    mockSearch.mockResolvedValue({
      records: [{ recordId: 'R1', messageType: '3001', messageName: 'Q', amount: null, dataCount: 1, entryMethod: 'API_CALL', createTime: '2026-04-18T10:00:00' } as any],
      total: 1, pageNum: 1, pageSize: 10, totalPages: 1,
    });
    const wrapper = mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    vm.onView(vm.page.records[0]);
    expect(vm.drawerVisible).toBe(true);
    expect(vm.drawerRecord).toBeTruthy();
  });

  it('surfaces error via ElMessage when search fails', async () => {
    // Use `new Error(...)` instead of a plain `{code, message}` object: Node 24's
    // util.inspect has a stack-overflow on reactive objects containing CJK strings,
    // which the other tests' happy-path mocks do not trigger (they resolve, not reject).
    // Don't "normalize" this to `mockRejectedValue({code: '...', message: '...'})`.
    mockSearch.mockRejectedValue(new Error('boom'));
    mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    expect(mockSearch).toHaveBeenCalled();
    expect(ElMessage.error).toHaveBeenCalledWith('boom');
  });

  it('renders selection column and disabled batch action with MockBadge', async () => {
    mockSearch.mockResolvedValue({
      records: [{ recordId: 'R1', messageType: '3001', messageName: 'Q', amount: null, dataCount: 1, entryMethod: 'API_CALL', createTime: '2026-04-18T10:00:00' } as any],
      total: 1, pageNum: 1, pageSize: 10, totalPages: 1,
    });
    const wrapper = mount(ReportRecordsPage, globalOpts);
    await flushPromises();
    expect(wrapper.find('[data-test="batch-action"]').attributes('disabled')).toBeDefined();
    expect(wrapper.text()).toContain('批量操作 P1 后启用');
    expect(wrapper.find('.el-table__header-wrapper .el-checkbox').exists()).toBe(true);
  });
});
