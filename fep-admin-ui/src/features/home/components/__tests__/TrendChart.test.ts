import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';

vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

import TrendChart from '../TrendChart.vue';

vi.mock('@/features/home/api/stats-api', () => ({
  statsApi: {
    getTrend: vi.fn().mockResolvedValue([
      { label: '04-10', sentCount: 100, receivedCount: 80 },
      { label: '04-11', sentCount: 120, receivedCount: 90 },
    ]),
  },
}));

import { statsApi } from '@/features/home/api/stats-api';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('TrendChart', () => {
  it('fetches trend with THIS_WEEK range on mount', async () => {
    mount(TrendChart);
    await flush();
    expect(statsApi.getTrend).toHaveBeenCalledWith('THIS_WEEK');
  });
});
