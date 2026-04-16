import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import SearchForm from '../SearchForm.vue';

describe('SearchForm', () => {
  it('renders search and reset buttons', () => {
    const wrapper = mount(SearchForm, {
      global: { plugins: [ElementPlus] },
    });
    const buttons = wrapper.findAll('button');
    const texts = buttons.map((b) => b.text());
    expect(texts).toContain('搜索');
    expect(texts).toContain('重置');
  });

  it('emits search event when search button is clicked', async () => {
    const wrapper = mount(SearchForm, {
      global: { plugins: [ElementPlus] },
    });
    const searchBtn = wrapper.findAll('button').find((b) => b.text() === '搜索')!;
    await searchBtn.trigger('click');
    expect(wrapper.emitted('search')).toHaveLength(1);
  });

  it('emits reset then search when reset button is clicked', async () => {
    const wrapper = mount(SearchForm, {
      global: { plugins: [ElementPlus] },
    });
    const resetBtn = wrapper.findAll('button').find((b) => b.text() === '重置')!;
    await resetBtn.trigger('click');
    expect(wrapper.emitted('reset')).toHaveLength(1);
    expect(wrapper.emitted('search')).toHaveLength(1);
    // reset fires before search
    const allEvents = Object.keys(wrapper.emitted());
    expect(allEvents).toContain('reset');
    expect(allEvents).toContain('search');
  });

  it('renders slot content inside the form', () => {
    const wrapper = mount(SearchForm, {
      global: { plugins: [ElementPlus] },
      slots: { default: '<span class="custom-field">Name</span>' },
    });
    expect(wrapper.find('.custom-field').exists()).toBe(true);
    expect(wrapper.find('.custom-field').text()).toBe('Name');
  });
});
