// src/features/submission/views/__tests__/ReportViewPage.test.ts
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createRouter, createMemoryHistory } from 'vue-router';
import ReportViewPage from '../ReportViewPage.vue';
import { subReportApi } from '../../api/sub-report-api';
import { subMessageSummaryApi } from '../../api/sub-message-summary-api';
import { makeRecord } from '../../__tests__/fixtures/record';

vi.mock('../../api/sub-report-api', () => ({
  subReportApi: { getByMessageType: vi.fn(), getTrend: vi.fn() },
}));
vi.mock('../../api/sub-message-summary-api', () => ({
  subMessageSummaryApi: { getSummary: vi.fn() },
}));
vi.mock('@/features/biz-data/api/biz-message-definition-api', () => ({
  bizMessageDefinitionApi: {
    search: vi
      .fn()
      .mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 1, totalPages: 0 }),
  },
}));

// Stub ViewTrendChart: echarts + jsdom cannot render canvas (zrender calls
// `.clearRect()` on a null 2D context), which spams Node's util.inspect with
// a reactive proxy containing CJK strings until it stack-overflows. The stub
// preserves contract (same props) but avoids echarts init entirely.
vi.mock('../../components/ViewTrendChart.vue', () => ({
  default: {
    name: 'ViewTrendChart',
    props: ['points'],
    template: '<div class="view-trend-chart-stub" />',
  },
}));
const mockByType = vi.mocked(subReportApi.getByMessageType);
const mockTrend = vi.mocked(subReportApi.getTrend);
const mockSummary = vi.mocked(subMessageSummaryApi.getSummary);

const buildRouter = () =>
  createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/report/view', component: ReportViewPage }],
  });

beforeEach(() => {
  mockByType.mockReset();
  mockTrend.mockReset();
  mockSummary.mockReset();
});

describe('ReportViewPage', () => {
  it('shows empty guide when messageType query missing', async () => {
    const router = buildRouter();
    await router.push('/report/view');
    const wrapper = mount(ReportViewPage, { global: { plugins: [ElementPlus, router] } });
    await flushPromises();
    expect(wrapper.text()).toContain('请从报文数据列表或报送信息列表');
  });

  it('fires 3 parallel API calls when messageType provided', async () => {
    mockSummary.mockResolvedValue([
      {
        messageType: '3001',
        messageName: 'Q',
        totalCount: 10,
        pushedCount: 5,
        pendingCount: 5,
        businessTypeId: '',
      } as any,
    ]);
    mockByType.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });
    mockTrend.mockResolvedValue([{ period: '2026-01', count: 3 }]);
    const router = buildRouter();
    await router.push('/report/view?messageType=3001');
    mount(ReportViewPage, { global: { plugins: [ElementPlus, router] } });
    await flushPromises();
    expect(mockSummary).toHaveBeenCalledTimes(1);
    expect(mockByType).toHaveBeenCalledWith('3001', 1, 10);
    expect(mockTrend).toHaveBeenCalledWith('3001');
  });

  it('extracts totalCount/pushedCount from summary filter', async () => {
    mockSummary.mockResolvedValue([
      {
        messageType: '9999',
        totalCount: 99,
        pushedCount: 50,
        pendingCount: 49,
        messageName: 'X',
        businessTypeId: '',
      } as any,
      {
        messageType: '3001',
        totalCount: 20,
        pushedCount: 15,
        pendingCount: 5,
        messageName: 'Q',
        businessTypeId: '',
      } as any,
    ]);
    mockByType.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });
    mockTrend.mockResolvedValue([]);
    const router = buildRouter();
    await router.push('/report/view?messageType=3001');
    const wrapper = mount(ReportViewPage, { global: { plugins: [ElementPlus, router] } });
    await flushPromises();
    const vm = wrapper.vm as any;
    expect(vm.totalCount).toBe(20);
    expect(vm.pushedCount).toBe(15);
    expect(vm.messageName).toBe('Q');
  });

  it('watch({immediate: true}) triggers loadAll once on initial mount', async () => {
    mockSummary.mockResolvedValue([]);
    mockByType.mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 });
    mockTrend.mockResolvedValue([]);
    const router = buildRouter();
    await router.push('/report/view?messageType=3001');
    mount(ReportViewPage, { global: { plugins: [ElementPlus, router] } });
    await flushPromises();
    expect(mockByType).toHaveBeenCalledTimes(1);
  });

  it('onFilterApply narrows filteredRecords by entryMethod', async () => {
    mockSummary.mockResolvedValue([]);
    mockByType.mockResolvedValue({
      records: [
        makeRecord({ recordId: 'R1', entryMethod: 'API_CALL', createTime: '2026-04-10T00:00:00' }),
        makeRecord({ recordId: 'R2', entryMethod: 'MANUAL_ENTRY', createTime: '2026-04-12T00:00:00' }),
      ],
      total: 2,
      pageNum: 1,
      pageSize: 10,
      totalPages: 1,
    });
    mockTrend.mockResolvedValue([]);
    const router = buildRouter();
    await router.push('/report/view?messageType=3001');
    const wrapper = mount(ReportViewPage, { global: { plugins: [ElementPlus, router] } });
    await flushPromises();
    const vm = wrapper.vm as any;
    vm.onFilterApply({ dataType: '', reportType: 'API_CALL', dateRange: null });
    expect(vm.filteredRecords).toHaveLength(1);
    expect(vm.filteredRecords[0].recordId).toBe('R1');
  });
});
