// src/features/submission/views/__tests__/ReportPushPage.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import ReportPushPage from '../ReportPushPage.vue';
import { subReportApi } from '../../api/sub-report-api';

vi.mock('../../api/sub-report-api', () => ({
  subReportApi: {
    searchRecords: vi.fn(),
    triggerPush: vi.fn(),
    getBlocked: vi.fn(),
  },
}));
const mockSearch = vi.mocked(subReportApi.searchRecords);
const mockTriggerPush = vi.mocked(subReportApi.triggerPush);
const mockGetBlocked = vi.mocked(subReportApi.getBlocked);

const mockRouterPush = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush }),
}));

// Mock ElMessage so notification paths are observable and do not render.
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

function makeRecord(overrides: Partial<Record<string, any>> = {}): any {
  return {
    recordId: 'R1',
    messageType: '3001',
    messageName: '供应链融资申请',
    businessTypeId: null,
    submitterName: null,
    businessNo: null,
    amount: '1234.56',
    dataCount: 10,
    entryMethod: 'API_CALL',
    entryBy: null,
    pushStatus: 'PENDING',
    pushTime: null,
    errorMessage: null,
    sortOrder: 0,
    createTime: '2026-04-18T10:00:00',
    updateTime: '2026-04-18T10:00:00',
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  mockGetBlocked.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });
});

describe('ReportPushPage', () => {
  it('renders relation card with MockBadge and CTA button, loads PENDING-only records', async () => {
    mockSearch.mockResolvedValue({
      records: [
        makeRecord({ recordId: 'R1', pushStatus: 'PENDING' }),
        makeRecord({ recordId: 'R2', pushStatus: 'PUSHED' }),
        makeRecord({ recordId: 'R3', pushStatus: 'FAILED' }),
      ],
      total: 3, pageNum: 1, pageSize: 500, totalPages: 1,
    });
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    // Relation card + MockBadge + CTA
    expect(wrapper.text()).toContain('推送关联');
    expect(wrapper.text()).toContain('真实 TLQ 推送 P1 就绪后启用');
    expect(wrapper.text()).toContain('前往输出接口管理');
    // searchRecords called with pageSize 500
    expect(mockSearch).toHaveBeenCalledWith({ pageNum: 1, pageSize: 500 });
    // Only the PENDING record retained client-side
    const vm = wrapper.vm as any;
    expect(vm.pendingRecords).toHaveLength(1);
    expect(vm.pendingRecords[0].recordId).toBe('R1');
  });

  it('renders the 500-row pending limit alert with ticket #11 wording', async () => {
    mockSearch.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 500, totalPages: 0 });
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    const alert = wrapper.find('[data-test="pending-limit-alert"]');
    expect(alert.exists()).toBe(true);
    expect(alert.text()).toContain('最近 500 条记录中的 PENDING 子集');
    expect(alert.text()).toContain('ticket #11');
  });

  it('onTriggerPush success: calls triggerPush with selected ids, shows success message, clears selection', async () => {
    mockSearch.mockResolvedValue({
      records: [makeRecord({ recordId: 'R1', pushStatus: 'PENDING' })],
      total: 1, pageNum: 1, pageSize: 500, totalPages: 1,
    });
    mockTriggerPush.mockResolvedValue([]);
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    // Simulate selection of 2 rows
    vm.selected = [
      makeRecord({ recordId: 'R1' }),
      makeRecord({ recordId: 'R2' }),
    ];
    await vm.onTriggerPush();
    expect(mockTriggerPush).toHaveBeenCalledWith(['R1', 'R2']);
    expect(ElMessage.success).toHaveBeenCalledWith('已触发 2 条推送');
    // Selection cleared post-push
    expect(vm.selected).toHaveLength(0);
    // searchRecords called again as part of reload
    expect(mockSearch).toHaveBeenCalledTimes(2);
  });

  it('onTriggerPush BIZ_5003 error: shows warning "没有待推送的记录"', async () => {
    mockSearch.mockResolvedValue({
      records: [makeRecord({ recordId: 'R1', pushStatus: 'PENDING' })],
      total: 1, pageNum: 1, pageSize: 500, totalPages: 1,
    });
    // Use `new Error(...)` with extra `code` property instead of plain object: Node 24's
    // util.inspect has a stack-overflow on reactive objects containing CJK strings,
    // which the other tests' happy-path mocks do not trigger (they resolve, not reject).
    // Don't "normalize" this to `mockRejectedValue({code: 'BIZ_5003', message: '...'})`.
    const err: any = new Error('no pending');
    err.code = 'BIZ_5003';
    mockTriggerPush.mockRejectedValue(err);
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    vm.selected = [makeRecord({ recordId: 'R1' })];
    await vm.onTriggerPush();
    expect(ElMessage.warning).toHaveBeenCalledWith('没有待推送的记录');
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('onTriggerPush generic error: shows ElMessage.error with err.message', async () => {
    mockSearch.mockResolvedValue({
      records: [makeRecord({ recordId: 'R1', pushStatus: 'PENDING' })],
      total: 1, pageNum: 1, pageSize: 500, totalPages: 1,
    });
    // Same Node 24 CJK workaround — use `new Error(...)` for rejection.
    mockTriggerPush.mockRejectedValue(new Error('boom'));
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    const vm = wrapper.vm as any;
    vm.selected = [makeRecord({ recordId: 'R1' })];
    await vm.onTriggerPush();
    expect(ElMessage.error).toHaveBeenCalledWith('boom');
    expect(ElMessage.warning).not.toHaveBeenCalled();
  });

  it('loadPending error: shows ElMessage.error with err.message', async () => {
    // Node 24 CJK workaround: use `new Error(...)` for rejection.
    mockSearch.mockRejectedValue(new Error('load fail'));
    mount(ReportPushPage, globalOpts);
    await flushPromises();
    expect(ElMessage.error).toHaveBeenCalledWith('load fail');
  });

  it('trigger-push button is disabled when no rows are selected', async () => {
    mockSearch.mockResolvedValue({
      records: [makeRecord({ recordId: 'R1', pushStatus: 'PENDING' })],
      total: 1, pageNum: 1, pageSize: 500, totalPages: 1,
    });
    const wrapper = mount(ReportPushPage, globalOpts);
    await flushPromises();
    const btn = wrapper.find('[data-test="trigger-push"]');
    expect(btn.exists()).toBe(true);
    expect(btn.attributes('disabled')).toBeDefined();
    expect(btn.text()).toContain('触发推送（选中 0 条）');
  });
});
