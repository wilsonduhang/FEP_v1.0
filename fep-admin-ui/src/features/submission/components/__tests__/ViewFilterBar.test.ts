// src/features/submission/components/__tests__/ViewFilterBar.test.ts
import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import ViewFilterBar from '../ViewFilterBar.vue';

describe('ViewFilterBar', () => {
  it('initializes with empty default filter state', () => {
    const w = mount(ViewFilterBar, { global: { plugins: [ElementPlus] } });
    const vm = w.vm as any;
    expect(vm.filter.dataType).toBe('');
    expect(vm.filter.reportType).toBe('');
    expect(vm.filter.dateRange).toBeNull();
  });

  it('emits apply event with current filter state', async () => {
    const w = mount(ViewFilterBar, { global: { plugins: [ElementPlus] } });
    const vm = w.vm as any;
    vm.filter.reportType = 'API_CALL';
    vm.apply();
    expect(w.emitted('apply')).toBeTruthy();
    expect(w.emitted('apply')![0][0]).toMatchObject({ reportType: 'API_CALL' });
  });

  it('reset clears filter state and emits apply', () => {
    const w = mount(ViewFilterBar, { global: { plugins: [ElementPlus] } });
    const vm = w.vm as any;
    vm.filter.reportType = 'MANUAL_ENTRY';
    vm.reset();
    expect(vm.filter.reportType).toBe('');
    expect(w.emitted('apply')).toBeTruthy();
  });
});
