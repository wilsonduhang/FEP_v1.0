import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import TlqQueueCreateDialog from '../TlqQueueCreateDialog.vue';
import { tlqQueueApi } from '../../api/tlq-queue-api';

vi.mock('../../api/tlq-queue-api');

/**
 * Unit tests for {@link TlqQueueCreateDialog} (P7.2d Task 5).
 *
 * <p>Follows the peer {@code TlqNodeEditDialog.test.ts} pattern (post-fix
 * {@code 65dd217}): user-visible behaviour is triggered via a real DOM click
 * on the 确定 button found under {@code document.body} (Element Plus teleports
 * the dialog root outside the component host), honouring the Global Test Red
 * Line #1 — no {@code wrapper.vm.onSubmit()} or other method bypass.</p>
 *
 * <p>{@code formRef.validate} is stubbed on the exposed ref because jsdom does
 * not reliably register teleported {@code el-form-item} rule wiring; form
 * state is seeded through the exposed {@code form} reactive. These are
 * state-level shortcuts, not behaviour invocations.</p>
 */

describe('TlqQueueCreateDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(ElMessage, 'success').mockImplementation(
      () => ({}) as ReturnType<typeof ElMessage.success>,
    );
    vi.spyOn(ElMessage, 'error').mockImplementation(
      () => ({}) as ReturnType<typeof ElMessage.error>,
    );
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
  });

  it('renders the PRD §3.1.2 naming hint alert when open', async () => {
    mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('新建队列');
    expect(body).toContain('QLOCAL.HNDEMP');
    expect(body).toContain('PRD §3.1.2');
  });

  it('declares required and length rules for the form fields', async () => {
    const wrapper = mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<
        string,
        Array<{ required?: boolean; min?: number; max?: number; trigger?: string }>
      >;
    };
    expect(vm.rules.queueName?.find((r) => r.required)?.required).toBe(true);
    const queueLen = vm.rules.queueName?.find((r) => r.min !== undefined);
    expect(queueLen?.min).toBe(1);
    expect(queueLen?.max).toBe(100);
    expect(vm.rules.channelType?.find((r) => r.required)?.required).toBe(true);
    expect(vm.rules.queueType?.find((r) => r.required)?.required).toBe(true);
    expect(vm.rules.description?.find((r) => r.max !== undefined)?.max).toBe(500);
  });

  it('does not call createQueue when validation rejects', async () => {
    const wrapper = mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
    };
    // EP rejects validate() with invalidFields when rules fail — simulate.
    vm.formRef.validate = () =>
      Promise.reject({ queueName: [{ message: '队列名称不能为空' }] });

    // Drive 确定 via DOM click (Red Line #1) — the stubbed rejection must
    // short-circuit before any API call is issued.
    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.createQueue).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('sends create request with nodeId from prop + form fields and emits success', async () => {
    vi.mocked(tlqQueueApi.createQueue).mockResolvedValue({
      queueId: 'Q1',
      nodeId: 'N1',
      queueName: 'QLOCAL.HNDEMP',
      channelType: 'REALTIME',
      queueType: 'LOCAL',
      queueStatus: 'ENABLED',
      description: null,
      createTime: '2026-04-20T10:00:00',
      updateTime: '2026-04-20T10:00:00',
    });
    const wrapper = mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: {
        queueName: string;
        channelType: string;
        queueType: string;
        description: string;
      };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.queueName = 'QLOCAL.HNDEMP';
    vm.form.channelType = 'REALTIME';
    vm.form.queueType = 'LOCAL';
    vm.form.description = '';
    vm.formRef.validate = () => Promise.resolve(true);

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.createQueue).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(tlqQueueApi.createQueue).mock.calls[0]![0];
    expect(payload).toEqual({
      nodeId: 'N1',
      queueName: 'QLOCAL.HNDEMP',
      channelType: 'REALTIME',
      queueType: 'LOCAL',
      description: undefined,
    });
    expect(ElMessage.success).toHaveBeenCalledWith('队列创建成功');
    expect(wrapper.emitted('success')).toBeTruthy();
    expect(wrapper.emitted('success')!.length).toBe(1);
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });

  it('surfaces ElMessage.error on backend 409 conflict and does not emit success', async () => {
    vi.mocked(tlqQueueApi.createQueue).mockRejectedValue(new Error('队列名称已存在'));
    const wrapper = mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { queueName: string; channelType: string; queueType: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.queueName = 'QLOCAL.HNDEMP';
    vm.form.channelType = 'REALTIME';
    vm.form.queueType = 'LOCAL';
    vm.formRef.validate = () => Promise.resolve(true);

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '确定',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.createQueue).toHaveBeenCalledTimes(1);
    expect(ElMessage.error).toHaveBeenCalledWith('队列名称已存在');
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('emits update:modelValue(false) on cancel button click without calling API', async () => {
    const wrapper = mount(TlqQueueCreateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
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
    expect(tlqQueueApi.createQueue).not.toHaveBeenCalled();
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });
});
