import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import OutputInterfaceEditDialog from '../OutputInterfaceEditDialog.vue';
import type { OutputInterfaceResponse } from '../../api/sub-output-interface-api';

const mockRecord: OutputInterfaceResponse = {
  interfaceId: 'I-1',
  interfaceName: '对账回传',
  interfaceUrl: 'https://bank.example.com/recon',
  businessTypeId: '3000',
  authType: 'TOKEN',
  timeoutSeconds: 60,
  retryCount: 2,
  interfaceStatus: 'ENABLED',
  lastCallTime: null,
  callCount: 0,
  createTime: '2026-04-17T10:00:00',
  updateTime: '2026-04-17T10:00:00',
};

describe('OutputInterfaceEditDialog', () => {
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

  it('rejects empty interfaceName', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ required?: boolean; min?: number; max?: number }>>;
    };
    const rule = vm.rules.interfaceName?.find((r) => r.required);
    expect(rule?.required).toBe(true);
    const sizeRule = vm.rules.interfaceName?.find((r) => r.min !== undefined);
    expect(sizeRule?.min).toBe(1);
    expect(sizeRule?.max).toBe(30);
  });

  it('rejects invalid interfaceUrl (mailto fails http(s) pattern)', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ pattern?: RegExp }>>;
    };
    const patternRule = vm.rules.interfaceUrl?.find((r) => r.pattern);
    expect(patternRule?.pattern).toBeDefined();
    expect(patternRule!.pattern!.test('mailto:foo@bar.com')).toBe(false);
    expect(patternRule!.pattern!.test('https://example.com')).toBe(true);
    expect(patternRule!.pattern!.test('http://example.com')).toBe(true);
  });

  it('requires authType', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ required?: boolean }>>;
    };
    const rule = vm.rules.authType?.find((r) => r.required);
    expect(rule?.required).toBe(true);
  });

  it('enforces timeoutSeconds range 1-300', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ min?: number; max?: number; type?: string }>>;
    };
    const rangeRule = vm.rules.timeoutSeconds?.find((r) => r.min !== undefined);
    expect(rangeRule?.min).toBe(1);
    expect(rangeRule?.max).toBe(300);
    expect(rangeRule?.type).toBe('number');
  });

  it('enforces retryCount range 0-10', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ min?: number; max?: number; type?: string }>>;
    };
    const rangeRule = vm.rules.retryCount?.find((r) => r.min !== undefined);
    expect(rangeRule?.min).toBe(0);
    expect(rangeRule?.max).toBe(10);
    expect(rangeRule?.type).toBe('number');
  });

  it('emits save event with form payload on valid submit', async () => {
    const wrapper = mount(OutputInterfaceEditDialog, {
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
    expect(payload.interfaceName).toBe('对账回传');
    expect(payload.authType).toBe('TOKEN');
    expect(payload.timeoutSeconds).toBe(60);
    expect(payload.retryCount).toBe(2);
  });
});
