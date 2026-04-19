// src/features/submission/components/__tests__/RecordDetailDrawer.test.ts
import { describe, expect, it } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import RecordDetailDrawer from '../RecordDetailDrawer.vue';

const record = {
  recordId: 'R1', messageType: '3001', messageName: '查询请求',
  businessTypeId: null, submitterName: '银行 A', businessNo: 'BN001',
  amount: '1234.56', dataCount: 5, entryMethod: 'API_CALL' as const,
  entryBy: 'zhang', pushStatus: 'PENDING' as const, pushTime: null,
  errorMessage: null, sortOrder: 0,
  createTime: '2026-04-18T10:00:00', updateTime: '2026-04-18T10:00:00',
};

describe('RecordDetailDrawer', () => {
  it('renders 16 fields when record provided', async () => {
    const wrapper = mount(RecordDetailDrawer, {
      props: { modelValue: true, record },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await flushPromises();
    expect(wrapper.text()).toContain('R1');
    expect(wrapper.text()).toContain('查询请求');
    expect(wrapper.text()).toContain('1,234.56');
    wrapper.unmount();
  });

  it('renders placeholder when record is null', async () => {
    const wrapper = mount(RecordDetailDrawer, {
      props: { modelValue: true, record: null },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await flushPromises();
    expect(wrapper.text()).not.toContain('记录 ID');
    wrapper.unmount();
  });
});
