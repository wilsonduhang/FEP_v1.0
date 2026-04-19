// src/features/submission/components/__tests__/ViewTrendChart.test.ts
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import ViewTrendChart from '../ViewTrendChart.vue';

// Stub echarts init: jsdom has no canvas 2D context, so zrender's RAF loop
// bombs with `TypeError: Cannot read properties of null (reading 'clearRect')`
// — the loop keeps firing past the test file boundary and poisons the next
// file. Stubbing `init` to return a no-op ECharts-shaped object keeps the
// component mount path clean without touching production render logic.
vi.mock('echarts/core', async () => {
  const actual = await vi.importActual<typeof import('echarts/core')>('echarts/core');
  return {
    ...actual,
    init: vi.fn(() => ({
      setOption: vi.fn(),
      dispose: vi.fn(),
      resize: vi.fn(),
    })),
  };
});

describe('ViewTrendChart', () => {
  it('renders empty placeholder when points is empty', () => {
    const w = mount(ViewTrendChart, {
      props: { points: [] },
      global: { plugins: [ElementPlus] },
    });
    expect(w.text()).toContain('暂无数据');
  });

  it('renders chart container when points are present', () => {
    const w = mount(ViewTrendChart, {
      props: { points: [{ period: '2026-01', count: 10 }] },
      global: { plugins: [ElementPlus] },
    });
    expect(w.find('.chart').exists()).toBe(true);
  });
});
