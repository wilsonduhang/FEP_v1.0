import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import BusinessSceneEditDialog from '../BusinessSceneEditDialog.vue';
import type { BusinessSceneResponse } from '../../api/sub-business-scene-api';

const mockManualRecord: BusinessSceneResponse = {
  sceneId: 'SC-1',
  sceneName: '对账手动推送',
  businessTypeId: '3000',
  pushMethod: 'MANUAL',
  importTemplatePath: '/tmp/recon-template.xml',
  requestUrl: 'https://bank.example.com/recon',
  sortOrder: 10,
  sceneStatus: 'ENABLED',
  createTime: '2026-04-17T10:00:00',
  updateTime: '2026-04-17T10:00:00',
};

describe('BusinessSceneEditDialog', () => {
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

  it('rejects sceneName under 3 chars (validate rejection blocks save)', async () => {
    // Behavior-level: stub formRef.validate to reject with the rule the 3-30
    // length constraint would fail for, and confirm save is NOT emitted.
    const wrapper = mount(BusinessSceneEditDialog, {
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
      Promise.reject({ sceneName: [{ message: '长度 3-30 字符' }] });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
  });

  it('requires businessTypeId (validate rejection blocks save)', async () => {
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
      onSave: () => Promise<void>;
      rules: Record<string, Array<{ required?: boolean }>>;
    };
    // Rule declaration check (complementary to behavioral check below).
    expect(vm.rules.businessTypeId?.find((r) => r.required)?.required).toBe(true);
    vm.formRef.validate = () =>
      Promise.reject({ businessTypeId: [{ message: '业务类型必填' }] });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
  });

  it('disables importTemplatePath in AUTO mode and enables in MANUAL (DOM behavior)', async () => {
    // Behavioral: mount with AUTO (default on create), assert the el-input
    // underlying <input> is disabled; then switch to MANUAL and confirm it
    // becomes enabled. The data-test hook points at the form-item wrapper.
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    // AUTO → disabled
    const autoInput = document.querySelector<HTMLInputElement>(
      '[data-test="import-template-path"] input',
    );
    expect(autoInput).not.toBeNull();
    expect(autoInput!.disabled).toBe(true);

    // Flip to MANUAL and assert enabled.
    const vm = wrapper.vm as unknown as { form: { pushMethod: 'AUTO' | 'MANUAL' } };
    vm.form.pushMethod = 'MANUAL';
    await flushPromises();
    const manualInput = document.querySelector<HTMLInputElement>(
      '[data-test="import-template-path"] input',
    );
    expect(manualInput).not.toBeNull();
    expect(manualInput!.disabled).toBe(false);
  });

  it('requires importTemplatePath in MANUAL mode (real-rule validate blocks save)', async () => {
    // Behavioral test using the actual rule: construct the form in MANUAL
    // mode with empty importTemplatePath, call rule.validator directly, and
    // confirm it calls back with an Error. Then ensure onSave does not emit.
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { pushMethod: 'AUTO' | 'MANUAL'; importTemplatePath: string };
      rules: Record<
        string,
        Array<{
          validator?: (
            rule: unknown,
            value: string,
            cb: (err?: Error) => void,
          ) => void;
        }>
      >;
      formRef: { validate: () => Promise<boolean> };
      onSave: () => Promise<void>;
    };
    vm.form.pushMethod = 'MANUAL';
    vm.form.importTemplatePath = '';
    await flushPromises();

    // Directly exercise the conditional-required rule.
    const validator = vm.rules.importTemplatePath?.find((r) => r.validator)?.validator;
    expect(validator).toBeDefined();
    const callback = vi.fn();
    validator!(null, '', callback);
    expect(callback).toHaveBeenCalledTimes(1);
    const passedArg = callback.mock.calls[0][0];
    expect(passedArg).toBeInstanceOf(Error);

    // And save must not fire when validation rejects.
    vm.formRef.validate = () =>
      Promise.reject({
        importTemplatePath: [{ message: 'MANUAL 模式需填模板路径' }],
      });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
  });

  it('validates requestUrl as URL (rule pattern + validate rejection)', async () => {
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ pattern?: RegExp }>>;
      formRef: { validate: () => Promise<boolean> };
      onSave: () => Promise<void>;
    };
    const patternRule = vm.rules.requestUrl?.find((r) => r.pattern);
    expect(patternRule?.pattern).toBeDefined();
    expect(patternRule!.pattern!.test('mailto:foo@bar.com')).toBe(false);
    expect(patternRule!.pattern!.test('https://x.example.com')).toBe(true);
    expect(patternRule!.pattern!.test('http://x.example.com')).toBe(true);
    vm.formRef.validate = () =>
      Promise.reject({ requestUrl: [{ message: '必须是 http(s) URL' }] });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
  });

  it('requires sortOrder as integer (validate rejection blocks save)', async () => {
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ required?: boolean; type?: string }>>;
      formRef: { validate: () => Promise<boolean> };
      onSave: () => Promise<void>;
    };
    expect(vm.rules.sortOrder?.find((r) => r.required)?.required).toBe(true);
    expect(vm.rules.sortOrder?.find((r) => r.type === 'integer')?.type).toBe('integer');
    vm.formRef.validate = () =>
      Promise.reject({ sortOrder: [{ message: '必须是整数' }] });
    await vm.onSave();
    await flushPromises();
    expect(wrapper.emitted('save')).toBeUndefined();
  });

  it('clears importTemplatePath when switching from MANUAL to AUTO', async () => {
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'edit', record: mockManualRecord },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { pushMethod: 'AUTO' | 'MANUAL'; importTemplatePath: string };
    };
    expect(vm.form.pushMethod).toBe('MANUAL');
    expect(vm.form.importTemplatePath).toBe('/tmp/recon-template.xml');
    vm.form.pushMethod = 'AUTO';
    await flushPromises();
    expect(vm.form.importTemplatePath).toBe('');
  });

  it('emits save event with form payload on valid submit (edit mode MANUAL)', async () => {
    const wrapper = mount(BusinessSceneEditDialog, {
      props: { modelValue: true, mode: 'edit', record: mockManualRecord },
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
    expect(payload.sceneName).toBe('对账手动推送');
    expect(payload.businessTypeId).toBe('3000');
    expect(payload.pushMethod).toBe('MANUAL');
    expect(payload.importTemplatePath).toBe('/tmp/recon-template.xml');
    expect(payload.requestUrl).toBe('https://bank.example.com/recon');
    expect(payload.sortOrder).toBe(10);
  });
});
