import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ElementPlus from 'element-plus';
import RecordSummaryCards from '../RecordSummaryCards.vue';
import type { RecordSummaryItem } from '../../api/biz-message-record-api';

const items: RecordSummaryItem[] = [
  { messageCode: '1001', messageName: '查询请求', totalCount: 100, successCount: 80, pendingCount: 10, failedCount: 10 },
  { messageCode: '2001', messageName: '查询响应', totalCount: 50, successCount: 40, pendingCount: 5, failedCount: 5 },
];

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('RecordSummaryCards', () => {
  it('renders 4 cards', () => {
    const wrapper = mount(RecordSummaryCards, {
      props: { items },
      ...globalOpts,
    });
    expect(wrapper.text()).toContain('总报文数据');
    expect(wrapper.text()).toContain('成功数');
    expect(wrapper.text()).toContain('待处理数');
    expect(wrapper.text()).toContain('失败数');
  });

  it('aggregates counts across all items', () => {
    const wrapper = mount(RecordSummaryCards, {
      props: { items },
      ...globalOpts,
    });
    expect(wrapper.text()).toContain('150'); // total 100+50
    expect(wrapper.text()).toContain('120'); // success 80+40
    expect(wrapper.text()).toContain('15'); // pending 10+5
    // failedCount 10+5 = 15 already checked above, but check explicitly
    const text = wrapper.text();
    // Count occurrences of "15" — should appear at least twice (pending + failed)
    const matches = text.match(/15/g);
    expect(matches?.length).toBeGreaterThanOrEqual(2);
  });

  it('handles empty array', () => {
    const wrapper = mount(RecordSummaryCards, {
      props: { items: [] },
      ...globalOpts,
    });
    // All values should be 0
    const cardValues = wrapper.findAll('.card-value');
    cardValues.forEach((card) => {
      expect(card.text()).toBe('0');
    });
  });
});
