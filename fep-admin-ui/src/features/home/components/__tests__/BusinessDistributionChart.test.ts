import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';

vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

import BusinessDistributionChart from '../BusinessDistributionChart.vue';

vi.mock('@/features/home/api/stats-api', () => ({
  statsApi: {
    getDistribution: vi.fn().mockResolvedValue([
      { messageCode: '3101', messageName: '合同信息', count: 50, percentage: 50 },
      { messageCode: '3102', messageName: '对账信息', count: 30, percentage: 30 },
      { messageCode: '3103', messageName: '融资申请', count: 20, percentage: 20 },
    ]),
  },
}));

import { statsApi } from '@/features/home/api/stats-api';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('BusinessDistributionChart', () => {
  it('fetches distribution on mount', async () => {
    mount(BusinessDistributionChart);
    await flush();
    expect(statsApi.getDistribution).toHaveBeenCalledOnce();
  });
});
