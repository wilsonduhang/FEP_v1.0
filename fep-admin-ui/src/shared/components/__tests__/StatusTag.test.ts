import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import StatusTag from '../StatusTag.vue';

const statusMap = {
  DRAFT: { label: '草稿', type: 'info' as const },
  COMPLETED: { label: '已完成', type: 'success' as const },
};

describe('StatusTag', () => {
  it('renders mapped label and type for known value', () => {
    const wrapper = mount(StatusTag, {
      props: { value: 'DRAFT', mapping: statusMap },
      global: { plugins: [ElementPlus] },
    });
    expect(wrapper.text()).toBe('草稿');
    expect(wrapper.find('.el-tag--info').exists()).toBe(true);
  });

  it('falls back to raw value with info type for unknown value', () => {
    const wrapper = mount(StatusTag, {
      props: { value: 'UNKNOWN', mapping: statusMap },
      global: { plugins: [ElementPlus] },
    });
    expect(wrapper.text()).toBe('UNKNOWN');
    expect(wrapper.find('.el-tag--info').exists()).toBe(true);
  });

  it('reacts to value prop update', async () => {
    const wrapper = mount(StatusTag, {
      props: { value: 'DRAFT', mapping: statusMap },
      global: { plugins: [ElementPlus] },
    });
    await wrapper.setProps({ value: 'COMPLETED' });
    expect(wrapper.text()).toBe('已完成');
  });
});
