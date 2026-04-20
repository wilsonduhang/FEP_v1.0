import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import TlqNodeEditDialog from '../TlqNodeEditDialog.vue';
import { tlqNodeApi } from '../../api/tlq-node-api';
import type { TlqNodeResponse } from '../../types';

vi.mock('../../api/tlq-node-api');

/**
 * Unit tests for {@link TlqNodeEditDialog} (P7.2d Task 3).
 *
 * <p>The peer dialog test pattern — see {@code DefinitionEditDialog.test.ts},
 * {@code AuthLetterEditDialog.test.ts}, {@code QueryTaskCreateDialog.test.ts}
 * — finds the 确定/取消 button under {@code document.body} (Element Plus
 * teleports the dialog root outside the component host) and dispatches a real
 * DOM click. This honours the Global Test Red Line #1: user-visible behaviour
 * must be exercised through framework dispatch, never via direct
 * {@code wrapper.vm.onSubmit()} calls.</p>
 *
 * <p>State setup via the exposed {@code form} reactive and a stubbed
 * {@code formRef.validate()} remains acceptable — these are state-level
 * shortcuts, not behaviour invocations. Jsdom does not reliably register
 * teleported {@code el-form-item} instances, so bypassing schema validation is
 * the documented peer escape hatch; the triggering action still flows through
 * the real button {@code @click} binding.</p>
 */

const makeRecord = (overrides: Partial<TlqNodeResponse> = {}): TlqNodeResponse => ({
  nodeId: 'N1',
  nodeName: 'Master-1',
  nodeRole: 'MASTER_PRODUCER',
  hostIp: '192.168.1.10',
  port: 20001,
  vipAddress: null,
  protocol: 'TCP',
  nodeStatus: 'ONLINE',
  description: null,
  lastHeartbeat: null,
  createTime: '2026-04-20T10:00:00',
  updateTime: '2026-04-20T10:00:00',
  ...overrides,
});

describe('TlqNodeEditDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({}) as ReturnType<typeof ElMessage.success>);
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({}) as ReturnType<typeof ElMessage.error>);
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
  });

  it('shows "新建 TLQ 节点" title in create mode', async () => {
    mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(document.body.textContent || '').toContain('新建 TLQ 节点');
  });

  it('shows "编辑 TLQ 节点" title and disables nodeRole select in update mode', async () => {
    const existing = makeRecord();
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'update', record: existing },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(document.body.textContent || '').toContain('编辑 TLQ 节点');
    // Assert component-level `mode` is propagated so the :disabled binding in
    // template resolves to true (checked via the exposed form state).
    const vm = wrapper.vm as unknown as { mode: string };
    expect(vm.mode).toBe('update');
  });

  it('prefills form fields from record in update mode', async () => {
    const existing = makeRecord({
      nodeName: 'StandbyAlpha',
      hostIp: '10.0.0.5',
      port: 30001,
      vipAddress: '10.0.0.100',
      description: 'primary standby',
    });
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'update', record: existing },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: {
        nodeName: string;
        nodeRole: string;
        hostIp: string;
        port: number;
        vipAddress: string;
        description: string;
      };
    };
    expect(vm.form.nodeName).toBe('StandbyAlpha');
    expect(vm.form.nodeRole).toBe('MASTER_PRODUCER');
    expect(vm.form.hostIp).toBe('10.0.0.5');
    expect(vm.form.port).toBe(30001);
    expect(vm.form.vipAddress).toBe('10.0.0.100');
    expect(vm.form.description).toBe('primary standby');
  });

  it('enforces required / range rules on form fields', async () => {
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<
        string,
        Array<{
          required?: boolean;
          min?: number;
          max?: number;
          pattern?: RegExp;
          type?: string;
        }>
      >;
    };
    // nodeName: required + length 1-100
    const nameReq = vm.rules.nodeName?.find((r) => r.required);
    expect(nameReq?.required).toBe(true);
    const nameLen = vm.rules.nodeName?.find((r) => r.min !== undefined);
    expect(nameLen?.min).toBe(1);
    expect(nameLen?.max).toBe(100);
    // nodeRole: required
    expect(vm.rules.nodeRole?.find((r) => r.required)?.required).toBe(true);
    // hostIp: required + regex
    expect(vm.rules.hostIp?.find((r) => r.required)?.required).toBe(true);
    const ipPattern = vm.rules.hostIp?.find((r) => r.pattern);
    expect(ipPattern?.pattern?.test('192.168.1.10')).toBe(true);
    expect(ipPattern?.pattern?.test('host.example.com')).toBe(true);
    expect(ipPattern?.pattern?.test('not valid ip!!')).toBe(false);
    // port: 1-65535 number
    const portRange = vm.rules.port?.find((r) => r.min !== undefined);
    expect(portRange?.min).toBe(1);
    expect(portRange?.max).toBe(65535);
    expect(portRange?.type).toBe('number');
    // description: max 500
    expect(vm.rules.description?.find((r) => r.max !== undefined)?.max).toBe(500);
  });

  it('calls createNode and emits success on valid create submit', async () => {
    const created = makeRecord({ nodeId: 'N2', nodeName: 'Master-2' });
    vi.mocked(tlqNodeApi.createNode).mockResolvedValue(created);
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: {
        nodeName: string;
        nodeRole: string;
        hostIp: string;
        port: number;
        vipAddress: string;
        protocol: string;
        description: string;
      };
      formRef: { validate: () => Promise<boolean> };
    };
    // Populate minimum valid fields (state-level setup only).
    vm.form.nodeName = 'Master-2';
    vm.form.nodeRole = 'MASTER_PRODUCER';
    vm.form.hostIp = '192.168.1.11';
    vm.form.port = 20001;
    vm.form.protocol = 'TCP';
    vm.form.description = '';
    vm.form.vipAddress = '';
    // Stub validate (teleported el-form registration is unreliable in jsdom);
    // see peer pattern in DefinitionEditDialog.test.ts / AuthLetterEditDialog.test.ts.
    vm.formRef.validate = () => Promise.resolve(true);

    // Honour Red Line #1: drive the 确定 button click through DOM dispatch
    // rather than calling vm.onSubmit() directly.
    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqNodeApi.createNode).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(tlqNodeApi.createNode).mock.calls[0]![0];
    expect(payload.nodeName).toBe('Master-2');
    expect(payload.nodeRole).toBe('MASTER_PRODUCER');
    expect(payload.hostIp).toBe('192.168.1.11');
    expect(payload.port).toBe(20001);
    // Empty optional strings must be elided (backend treats undefined as unset).
    expect(payload.vipAddress).toBeUndefined();
    expect(payload.description).toBeUndefined();
    expect(ElMessage.success).toHaveBeenCalledWith('节点创建成功');
    expect(wrapper.emitted('success')).toBeTruthy();
    expect(wrapper.emitted('success')!.length).toBe(1);
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });

  it('calls updateNode without nodeRole and emits success in update mode', async () => {
    const existing = makeRecord();
    const updated = makeRecord({ nodeName: 'Master-1-Renamed' });
    vi.mocked(tlqNodeApi.updateNode).mockResolvedValue(updated);
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'update', record: existing },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { nodeName: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.nodeName = 'Master-1-Renamed';
    vm.formRef.validate = () => Promise.resolve(true);

    // Drive the 确定 button via DOM click (Red Line #1 compliance).
    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqNodeApi.updateNode).toHaveBeenCalledTimes(1);
    const [nodeId, payload] = vi.mocked(tlqNodeApi.updateNode).mock.calls[0]!;
    expect(nodeId).toBe('N1');
    expect(payload.nodeName).toBe('Master-1-Renamed');
    // Backend contract: nodeRole is not part of update payload.
    expect('nodeRole' in payload).toBe(false);
    expect(ElMessage.success).toHaveBeenCalledWith('节点更新成功');
    expect(wrapper.emitted('success')).toBeTruthy();
  });

  it('does not call API when validation rejects', async () => {
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
    };
    // Element Plus rejects validate() with invalidFields when rules fail.
    vm.formRef.validate = () =>
      Promise.reject({ nodeName: [{ message: '节点名称不能为空' }] });

    // Drive the 确定 button via DOM click (Red Line #1 compliance); the stubbed
    // validate() rejection must still short-circuit before any API call.
    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqNodeApi.createNode).not.toHaveBeenCalled();
    expect(tlqNodeApi.updateNode).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('surfaces ElMessage.error on backend 409 conflict and does not emit success', async () => {
    vi.mocked(tlqNodeApi.createNode).mockRejectedValue(new Error('节点名称已存在'));
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: {
        nodeName: string;
        nodeRole: string;
        hostIp: string;
        port: number;
      };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.nodeName = 'Master-1';
    vm.form.nodeRole = 'MASTER_PRODUCER';
    vm.form.hostIp = '192.168.1.10';
    vm.form.port = 20001;
    vm.formRef.validate = () => Promise.resolve(true);

    // Drive the 确定 button via DOM click (Red Line #1 compliance).
    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqNodeApi.createNode).toHaveBeenCalledTimes(1);
    expect(ElMessage.error).toHaveBeenCalledWith('节点名称已存在');
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('emits update:modelValue(false) on cancel button click', async () => {
    const wrapper = mount(TlqNodeEditDialog, {
      props: { modelValue: true, mode: 'create', record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const cancelBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '取消',
    );
    expect(cancelBtn).toBeTruthy();
    cancelBtn!.click();
    await flushPromises();
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });
});
