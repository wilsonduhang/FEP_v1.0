import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';

vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

import StatusDistributionChart from '../StatusDistributionChart.vue';

vi.mock('@/features/home/api/stats-api', () => ({
  statsApi: {
    getStatusDistribution: vi.fn().mockResolvedValue([
      { status: 'SUCCESS', count: 900, percentage: 90 },
      { status: 'FAILED', count: 50, percentage: 5 },
      { status: 'IN_PROCESS', count: 50, percentage: 5 },
    ]),
  },
}));

import { statsApi } from '@/features/home/api/stats-api';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('StatusDistributionChart', () => {
  it('fetches status distribution on mount', async () => {
    mount(StatusDistributionChart);
    await flush();
    expect(statsApi.getStatusDistribution).toHaveBeenCalledOnce();
  });
});
