// src/features/submission/components/__tests__/ViewStatsCards.test.ts
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import ViewStatsCards from '../ViewStatsCards.vue';

describe('ViewStatsCards', () => {
  it('renders both totalCount and pushedCount with thousand separators', () => {
    const w = mount(ViewStatsCards, {
      props: { totalCount: 1234, pushedCount: 567 },
      global: { plugins: [ElementPlus] },
    });
    expect(w.text()).toContain('1,234');
    expect(w.text()).toContain('567');
    expect(w.text()).toContain('总数据量');
    expect(w.text()).toContain('已推送数据量');
  });

  it('handles zero values gracefully', () => {
    const w = mount(ViewStatsCards, {
      props: { totalCount: 0, pushedCount: 0 },
      global: { plugins: [ElementPlus] },
    });
    expect(w.findAll('.stat-value')).toHaveLength(2);
  });
});
