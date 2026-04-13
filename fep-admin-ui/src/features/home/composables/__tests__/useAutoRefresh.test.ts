/* eslint-disable vue/one-component-per-file */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { defineComponent, onMounted } from 'vue';
import { mount } from '@vue/test-utils';
import { useAutoRefresh } from '../useAutoRefresh';

describe('useAutoRefresh', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('calls loader immediately and then per interval', async () => {
    const loader = vi.fn().mockResolvedValue(undefined);
    const Comp = defineComponent({
      setup() {
        const r = useAutoRefresh(loader, 1000);
        onMounted(() => r.start());
        return {};
      },
      template: '<div />',
    });
    mount(Comp);
    await Promise.resolve();
    expect(loader).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(1000);
    expect(loader).toHaveBeenCalledTimes(2);

    vi.advanceTimersByTime(2000);
    expect(loader).toHaveBeenCalledTimes(4);
  });

  it('stop clears interval', async () => {
    const loader = vi.fn().mockResolvedValue(undefined);
    const Comp = defineComponent({
      setup() {
        const r = useAutoRefresh(loader, 1000);
        onMounted(() => {
          r.start();
          r.stop();
        });
        return {};
      },
      template: '<div />',
    });
    mount(Comp);
    await Promise.resolve();
    expect(loader).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(5000);
    expect(loader).toHaveBeenCalledTimes(1);
  });

  it('unmount automatically stops', async () => {
    const loader = vi.fn().mockResolvedValue(undefined);
    const Comp = defineComponent({
      setup() {
        const r = useAutoRefresh(loader, 1000);
        onMounted(() => r.start());
        return {};
      },
      template: '<div />',
    });
    const w = mount(Comp);
    await Promise.resolve();
    w.unmount();
    vi.advanceTimersByTime(5000);
    expect(loader).toHaveBeenCalledTimes(1);
  });
});
