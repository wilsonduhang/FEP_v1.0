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

  it('renders backend-provided pendingCount (not derived from total - pushed)', () => {
    // Deliberate divergence: pendingCount=7 while totalCount-pushedCount=20.
    // If the template still did the subtraction we would see "20"; since we
    // render pendingCount directly the card must show "7". This pins the UI
    // to the backend semantics where PUSHING/FAILED are excluded from pending.
    const divergentItems: MessageSummaryItem[] = [
      {
        messageType: '3001',
        messageName: '查询请求',
        businessTypeId: 'BIZ001',
        totalCount: 100,
        pushedCount: 80,
        pendingCount: 7,
      },
    ];
    const wrapper = mount(SubMessageSummaryCards, {
      ...globalOpts,
      props: { items: divergentItems },
    });
    const card = wrapper.findAll('.summary-card')[0];
    expect(card.text()).toContain('待推送：');
    expect(card.text()).toContain('7');
    expect(card.text()).not.toContain('待推送：20');
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
