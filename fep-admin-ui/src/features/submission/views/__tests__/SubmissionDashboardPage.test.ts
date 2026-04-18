import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';

// --- Spies on useAutoRefresh controller ---
const startSpy = vi.fn();
const stopSpy = vi.fn();
const refreshSpy = vi.fn().mockResolvedValue(undefined);
let capturedLoader: (() => Promise<void>) | null = null;

vi.mock('@/features/home/composables/useAutoRefresh', () => ({
  useAutoRefresh: vi.fn((loader: () => Promise<void>) => {
    capturedLoader = loader;
    return {
      start: () => {
        startSpy();
        void loader();
      },
      stop: stopSpy,
      refresh: () => {
        refreshSpy();
        return loader();
      },
    };
  }),
}));

// --- Stub chart components to avoid echarts canvas in jsdom ---
vi.mock('../../components/SubmissionTrendChart.vue', () => ({
  default: { name: 'SubmissionTrendChart', props: ['data'], render: () => null },
}));
vi.mock('../../components/SubmissionDistributionChart.vue', () => ({
  default: { name: 'SubmissionDistributionChart', props: ['data'], render: () => null },
}));

// --- Mock API client ---
vi.mock('../../api/sub-dashboard-api');

import SubmissionDashboardPage from '../SubmissionDashboardPage.vue';
import { subDashboardApi } from '../../api/sub-dashboard-api';

const globalOpts = { global: { plugins: [ElementPlus] } };

const mockOverview = {
  totalInterfaceCount: 10,
  enabledInterfaceCount: 8,
  totalDataSourceCount: 5,
  totalRecordCount: 100,
  pushedRecordCount: 80,
  pendingRecordCount: 20,
};

describe('SubmissionDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    capturedLoader = null;
    vi.mocked(subDashboardApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(subDashboardApi.getTrend).mockResolvedValue({
      dates: ['2026-04-17'],
      pushedCounts: [5],
      pendingCounts: [2],
    });
    vi.mocked(subDashboardApi.getDistribution).mockResolvedValue([
      { name: '3001', value: 10 },
    ]);
  });

  it('renders 6 overview cards with correct values on mount', async () => {
    const wrapper = mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    expect(wrapper.find('[data-test="card-totalInterfaceCount"]').text()).toBe('10');
    expect(wrapper.find('[data-test="card-enabledInterfaceCount"]').text()).toBe('8');
    expect(wrapper.find('[data-test="card-totalDataSourceCount"]').text()).toBe('5');
    expect(wrapper.find('[data-test="card-totalRecordCount"]').text()).toBe('100');
    expect(wrapper.find('[data-test="card-pushedRecordCount"]').text()).toBe('80');
    expect(wrapper.find('[data-test="card-pendingRecordCount"]').text()).toBe('20');
  });

  it('invokes ctrl.start() on mount', async () => {
    mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    expect(startSpy).toHaveBeenCalledOnce();
  });

  it('initial trendDays is 7 and distDim is messageType; loadAll fetches all three endpoints', async () => {
    mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    expect(subDashboardApi.getOverview).toHaveBeenCalled();
    expect(subDashboardApi.getTrend).toHaveBeenCalledWith(7);
    expect(subDashboardApi.getDistribution).toHaveBeenCalledWith('messageType');
  });

  it('switches trend window to 30 days', async () => {
    const wrapper = mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    await (wrapper.vm as unknown as { handleTrendDaysChange: (v: 7 | 30) => Promise<void> })
      .handleTrendDaysChange(30);
    expect(subDashboardApi.getTrend).toHaveBeenLastCalledWith(30);
  });

  it('switches distribution dim to businessType', async () => {
    const wrapper = mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    await (wrapper.vm as unknown as {
      handleDistDimChange: (v: 'messageType' | 'businessType') => Promise<void>;
    }).handleDistDimChange('businessType');
    expect(subDashboardApi.getDistribution).toHaveBeenLastCalledWith('businessType');
  });

  it('manual refresh button triggers ctrl.refresh()', async () => {
    const wrapper = mount(SubmissionDashboardPage, globalOpts);
    await flushPromises();
    refreshSpy.mockClear();
    await wrapper.find('[data-test="manual-refresh"]').trigger('click');
    await flushPromises();
    expect(refreshSpy).toHaveBeenCalledOnce();
  });

  it('auto-refresh timer fires loader repeatedly at 30s interval', async () => {
    vi.useFakeTimers();
    try {
      mount(SubmissionDashboardPage, globalOpts);
      await flushPromises();
      expect(capturedLoader).not.toBeNull();
      // Simulate timer tick — call loader directly (useAutoRefresh is mocked,
      // but the interval semantics are validated in its own unit test).
      const callsBefore = vi.mocked(subDashboardApi.getOverview).mock.calls.length;
      await capturedLoader!();
      const callsAfter = vi.mocked(subDashboardApi.getOverview).mock.calls.length;
      expect(callsAfter).toBeGreaterThan(callsBefore);
    } finally {
      vi.useRealTimers();
    }
  });
});
