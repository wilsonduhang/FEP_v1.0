import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import DataSourceEditDialog from '../DataSourceEditDialog.vue';
import type { DataSourceResponse } from '../../api/sub-data-source-api';

const mockRecord: DataSourceResponse = {
  sourceId: 'S-1',
  sourceName: '核心系统',
  logoPath: '/logo/core.png',
  contactAddress: '北京市朝阳区建国路 88 号',
  contactPhone: '13800138000',
  pushEnabled: true,
  contentType: 'application/json',
  clientId: 'client-001',
  sourceStatus: 'ENABLED',
  createTime: '2026-04-17T10:00:00',
  updateTime: '2026-04-17T10:00:00',
};

describe('DataSourceEditDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
  });

  it('rejects sourceName longer than 30 chars', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ required?: boolean; min?: number; max?: number }>>;
    };
    const requiredRule = vm.rules.sourceName?.find((r) => r.required);
    expect(requiredRule?.required).toBe(true);
    const sizeRule = vm.rules.sourceName?.find((r) => r.min !== undefined);
    expect(sizeRule?.min).toBe(1);
    expect(sizeRule?.max).toBe(30);
  });

  it('rejects contactAddress longer than 50 chars', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ required?: boolean; min?: number; max?: number }>>;
    };
    const requiredRule = vm.rules.contactAddress?.find((r) => r.required);
    expect(requiredRule?.required).toBe(true);
    const sizeRule = vm.rules.contactAddress?.find((r) => r.min !== undefined);
    expect(sizeRule?.min).toBe(1);
    expect(sizeRule?.max).toBe(50);
  });

  it('accepts contactPhone matching ^\\d{1,11}$ (e.g. 13800138000)', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ pattern?: RegExp }>>;
    };
    const patternRule = vm.rules.contactPhone?.find((r) => r.pattern);
    expect(patternRule?.pattern).toBeDefined();
    expect(patternRule!.pattern!.test('13800138000')).toBe(true);
    expect(patternRule!.pattern!.test('1')).toBe(true);
  });

  it('rejects contactPhone with non-digits (e.g. 138-0013)', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ pattern?: RegExp }>>;
    };
    const patternRule = vm.rules.contactPhone?.find((r) => r.pattern);
    expect(patternRule?.pattern).toBeDefined();
    expect(patternRule!.pattern!.test('138-0013')).toBe(false);
    expect(patternRule!.pattern!.test('138 0013')).toBe(false);
    expect(patternRule!.pattern!.test('abcdefg')).toBe(false);
    // 12 digits rejected (upper bound 11)
    expect(patternRule!.pattern!.test('123456789012')).toBe(false);
  });

  it('defaults pushEnabled=false on create', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as { form: { pushEnabled: boolean } };
    expect(vm.form.pushEnabled).toBe(false);
  });

  it('emits save event with form payload on valid submit (edit mode)', async () => {
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'edit', record: mockRecord },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    await (wrapper.vm as unknown as { onSave: () => Promise<void> }).onSave();
    await flushPromises();
    const savedEvents = wrapper.emitted('save');
    expect(savedEvents).toBeTruthy();
    expect(savedEvents!.length).toBe(1);
    const payload = (savedEvents![0] as unknown[])[0] as Record<string, unknown>;
    expect(payload.sourceName).toBe('核心系统');
    expect(payload.contactPhone).toBe('13800138000');
    expect(payload.pushEnabled).toBe(true);
  });

  it('does not emit save event when form validation rejects', async () => {
    // Behavior test: when Element Plus FormInstance.validate() rejects
    // (contract: invalid fields → Promise.reject(invalidFields)), onSave
    // must NOT emit `save`. Stub the exposed formRef with a rejecting
    // validate so this is a pure behavior test on onSave's logic,
    // independent of JSDOM field-registration under teleported el-dialog.
    const wrapper = mount(DataSourceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
      onSave: () => Promise<void>;
    };
    vm.formRef.validate = () =>
      Promise.reject({ sourceName: [{ message: '请输入数据源名称' }] });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
  });
});
