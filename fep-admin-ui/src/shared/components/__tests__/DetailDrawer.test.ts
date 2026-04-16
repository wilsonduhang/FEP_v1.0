import { describe, it, expect, afterEach } from 'vitest';
import { mount, VueWrapper } from '@vue/test-utils';
import { nextTick } from 'vue';
import ElementPlus from 'element-plus';
import DetailDrawer from '../DetailDrawer.vue';

describe('DetailDrawer', () => {
  let wrapper: VueWrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it('renders el-drawer when modelValue is true', async () => {
    wrapper = mount(DetailDrawer, {
      props: { modelValue: true, title: '详情' },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await nextTick();
    expect(document.querySelector('.el-drawer')).not.toBeNull();
  });

  it('renders title in the drawer header', async () => {
    wrapper = mount(DetailDrawer, {
      props: { modelValue: true, title: '测试标题' },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await nextTick();
    const title = document.querySelector('.el-drawer__title');
    expect(title).not.toBeNull();
    expect(title!.textContent).toContain('测试标题');
  });

  it('renders close button in the drawer header', async () => {
    wrapper = mount(DetailDrawer, {
      props: { modelValue: true, title: '详情' },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await nextTick();
    const closeBtn = document.querySelector('.el-drawer__close-btn');
    expect(closeBtn).not.toBeNull();
  });

  it('uses rtl direction for right-to-left drawer', async () => {
    wrapper = mount(DetailDrawer, {
      props: { modelValue: true, title: '详情' },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await nextTick();
    const drawer = document.querySelector('.el-drawer');
    expect(drawer).not.toBeNull();
    expect(drawer!.classList.contains('rtl')).toBe(true);
  });

  it('emits update:modelValue when drawer closes', async () => {
    wrapper = mount(DetailDrawer, {
      props: { modelValue: true, title: '详情' },
      global: { plugins: [ElementPlus] },
      attachTo: document.body,
    });
    await nextTick();
    const drawerComponent = wrapper.findComponent({ name: 'ElDrawer' });
    expect(drawerComponent.exists()).toBe(true);
    drawerComponent.vm.$emit('update:model-value', false);
    expect(wrapper.emitted('update:modelValue')).toBeTruthy();
    expect(wrapper.emitted('update:modelValue')![0]).toEqual([false]);
  });
});
