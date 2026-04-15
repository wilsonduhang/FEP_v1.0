import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';

// Stub vue-echarts to avoid jsdom canvas initialization when chart children mount.
vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

// Mock all home APIs to avoid network traffic.
vi.mock('@/features/home/api/todo-api', () => ({
  todoApi: {
    search: vi.fn().mockResolvedValue({ records: [], total: 0, pageNum: 1, pageSize: 10, totalPages: 0 }),
    countPending: vi.fn().mockResolvedValue(0),
    startProcessing: vi.fn(),
    complete: vi.fn(),
  },
}));
vi.mock('@/features/home/api/stats-api', () => ({
  statsApi: {
    getCards: vi.fn().mockResolvedValue({
      totalAmount: '0',
      successCount: 0,
      todayMessageCount: 0,
      exceptionCount: 0,
    }),
    getTrend: vi.fn().mockResolvedValue([]),
    getDistribution: vi.fn().mockResolvedValue([]),
    getStatusDistribution: vi.fn().mockResolvedValue([]),
  },
}));
vi.mock('@/features/home/api/shortcut-api', () => ({
  shortcutApi: {
    list: vi.fn().mockResolvedValue([]),
    reorder: vi.fn(),
    toggleVisibility: vi.fn(),
  },
}));

import HomePage from '../HomePage.vue';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
  await new Promise((r) => setTimeout(r, 0));
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('mounts and loads all 7 child components via useAutoRefresh', async () => {
    mount(HomePage, {
      global: { plugins: [createPinia(), ElementPlus] },
    });
    await flush();

    const { todoApi } = await import('@/features/home/api/todo-api');
    const { statsApi } = await import('@/features/home/api/stats-api');
    const { shortcutApi } = await import('@/features/home/api/shortcut-api');

    expect(todoApi.search).toHaveBeenCalled();
    expect(todoApi.countPending).toHaveBeenCalled();
    expect(statsApi.getCards).toHaveBeenCalled();
    expect(statsApi.getTrend).toHaveBeenCalled();
    expect(statsApi.getDistribution).toHaveBeenCalled();
    expect(statsApi.getStatusDistribution).toHaveBeenCalled();
    expect(shortcutApi.list).toHaveBeenCalled();
  });

  it('manual refresh button triggers another round of loads', async () => {
    const wrapper = mount(HomePage, {
      global: { plugins: [createPinia(), ElementPlus] },
    });
    await flush();

    const { statsApi } = await import('@/features/home/api/stats-api');
    const firstCount = (statsApi.getCards as ReturnType<typeof vi.fn>).mock.calls.length;

    await wrapper.find('.top-bar button').trigger('click');
    await flush();

    const secondCount = (statsApi.getCards as ReturnType<typeof vi.fn>).mock.calls.length;
    expect(secondCount).toBeGreaterThan(firstCount);
  });
});
