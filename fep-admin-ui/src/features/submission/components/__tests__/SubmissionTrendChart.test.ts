import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';

// Stub vue-echarts to avoid jsdom canvas init errors.
vi.mock('vue-echarts', () => ({
  default: { name: 'VChart', render: () => null },
}));

import SubmissionTrendChart from '../SubmissionTrendChart.vue';

describe('SubmissionTrendChart', () => {
  it('mounts and accepts trend data prop without throwing', () => {
    const wrapper = mount(SubmissionTrendChart, {
      props: {
        data: {
          dates: ['2026-04-11', '2026-04-12'],
          pushedCounts: [3, 5],
          pendingCounts: [1, 2],
        },
      },
    });
    expect(wrapper.exists()).toBe(true);
    // VChart stub is present
    expect(wrapper.findComponent({ name: 'VChart' }).exists()).toBe(true);
  });
});
