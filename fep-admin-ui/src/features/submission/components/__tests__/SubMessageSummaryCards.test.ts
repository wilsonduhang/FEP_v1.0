import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ElementPlus from 'element-plus';
import SubMessageSummaryCards from '../SubMessageSummaryCards.vue';
import type { MessageSummaryItem } from '../../api/sub-message-summary-api';

const items: MessageSummaryItem[] = [
  {
    messageType: '3001',
    messageName: '查询请求',
    businessTypeId: 'BIZ001',
    totalCount: 100,
    pushedCount: 80,
    pendingCount: 20,
  },
  {
    messageType: '3002',
    messageName: '查询响应',
    businessTypeId: 'BIZ001',
    totalCount: 50,
    pushedCount: 50,
    pendingCount: 0,
  },
];

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('SubMessageSummaryCards', () => {
  it('renders one card per item', () => {
    const wrapper = mount(SubMessageSummaryCards, { ...globalOpts, props: { items } });
    expect(wrapper.findAll('.summary-card')).toHaveLength(2);
  });

  it('displays message type code and message name per card', () => {
    const wrapper = mount(SubMessageSummaryCards, { ...globalOpts, props: { items } });
    const text = wrapper.text();
    expect(text).toContain('3001');
    expect(text).toContain('查询请求');
    expect(text).toContain('3002');
    expect(text).toContain('查询响应');
    expect(text).toContain('BIZ001');
  });

  it('computes pending as totalCount - pushedCount', () => {
    const wrapper = mount(SubMessageSummaryCards, { ...globalOpts, props: { items } });
    const cards = wrapper.findAll('.summary-card');
    // Card 0: 100 - 80 = 20
    expect(cards[0].text()).toContain('20');
    // Card 1: 50 - 50 = 0
    expect(cards[1].text()).toContain('0');
  });

  it('emits navigate with messageType payload on card click', async () => {
    const wrapper = mount(SubMessageSummaryCards, { ...globalOpts, props: { items } });
    const cards = wrapper.findAll('.summary-card');
    await cards[0].trigger('click');
    expect(wrapper.emitted('navigate')?.[0]).toEqual([{ messageType: '3001' }]);
    await cards[1].trigger('click');
    expect(wrapper.emitted('navigate')?.[1]).toEqual([{ messageType: '3002' }]);
  });

  it('renders empty state placeholder when items is empty', () => {
    const wrapper = mount(SubMessageSummaryCards, {
      ...globalOpts,
      props: { items: [] },
    });
    expect(wrapper.text()).toContain('暂无数据');
    expect(wrapper.findAll('.summary-card')).toHaveLength(0);
  });
});
