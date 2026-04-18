import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import ElementPlus from 'element-plus';
import MockBadge from '../MockBadge.vue';

const g = { global: { plugins: [ElementPlus] } };

describe('MockBadge', () => {
  it('renders default text', () => {
    expect(mount(MockBadge, g).text()).toContain('Mock 模式');
  });

  it('honors small size prop', () => {
    expect(
      mount(MockBadge, { ...g, props: { size: 'small' } })
        .find('.el-tag--small')
        .exists(),
    ).toBe(true);
  });

  it('renders slot override', () => {
    expect(mount(MockBadge, { ...g, slots: { default: '待 TLQ 到位' } }).text()).toContain(
      '待 TLQ 到位',
    );
  });

  it('applies el-tag--info class', () => {
    expect(mount(MockBadge, g).find('.el-tag').classes()).toContain('el-tag--info');
  });

  it('exposes data-test="mock-badge" for E2E', () => {
    // Durable selector: the CSS class .mock-badge is also available today, but
    // data-test is guaranteed never to be renamed by a style refactor.
    const wrapper = mount(MockBadge, g);
    expect(wrapper.find('[data-test="mock-badge"]').exists()).toBe(true);
  });
});
