import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';

// Stub vue-echarts to avoid jsdom canvas init errors.
vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

import SubmissionDistributionChart from '../SubmissionDistributionChart.vue';

describe('SubmissionDistributionChart', () => {
  it('mounts and accepts distribution data prop without throwing', () => {
    const wrapper = mount(SubmissionDistributionChart, {
      props: {
        data: [
          { name: '3001', value: 12 },
          { name: '3002', value: 7 },
        ],
      },
    });
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent({ name: 'VChart' }).exists()).toBe(true);
  });
});
