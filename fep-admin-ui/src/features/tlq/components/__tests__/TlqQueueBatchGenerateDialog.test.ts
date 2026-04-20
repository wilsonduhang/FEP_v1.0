import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import TlqQueueBatchGenerateDialog from '../TlqQueueBatchGenerateDialog.vue';
import { tlqQueueApi } from '../../api/tlq-queue-api';

vi.mock('../../api/tlq-queue-api');

/**
 * Unit tests for {@link TlqQueueBatchGenerateDialog} (P7.2d Task 6).
 *
 * <p>Mirrors {@code TlqQueueCreateDialog.test.ts} pattern: user-visible
 * behaviour is triggered via real DOM click on the 生成 button (Red Line #1 —
 * no {@code wrapper.vm.onSubmit()} bypass). Form state is seeded through
 * exposed {@code form} reactive and {@code formRef.validate} is stubbed.</p>
 */

describe('TlqQueueBatchGenerateDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(ElMessage, 'success').mockImplementation(
      () => ({}) as ReturnType<typeof ElMessage.success>,
    );
    vi.spyOn(ElMessage, 'info').mockImplementation(
      () => ({}) as ReturnType<typeof ElMessage.info>,
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

  it('defaults organizationCode to HNDEMP center code A1000143000104', async () => {
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { organizationCode: string };
    };
    expect(vm.form.organizationCode).toBe('A1000143000104');
  });

  it('shows PRD §3.1.2 info alert text on open', async () => {
    mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('PRD §3.1.2');
    expect(body).toContain('9 条标准队列');
  });

  it('declares required + length rules for organizationCode', async () => {
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
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
    expect(vm.rules.organizationCode?.find((r) => r.required)?.required).toBe(true);
    const orgCodeLen = vm.rules.organizationCode?.find((r) => r.min !== undefined);
    expect(orgCodeLen?.min).toBe(1);
    expect(orgCodeLen?.max).toBe(50);
  });

  it('sends batch-generate request with nodeId + organizationCode when response.length > 0', async () => {
    const response = [
      { queueId: 'Q1', nodeId: 'N1', queueName: 'Q1', channelType: 'REALTIME' as const, queueType: 'LOCAL' as const, queueStatus: 'ENABLED' as const, description: null, createTime: '2026-04-20T10:00:00', updateTime: '2026-04-20T10:00:00' },
      { queueId: 'Q2', nodeId: 'N1', queueName: 'Q2', channelType: 'REALTIME' as const, queueType: 'REMOTE' as const, queueStatus: 'ENABLED' as const, description: null, createTime: '2026-04-20T10:00:00', updateTime: '2026-04-20T10:00:00' },
      { queueId: 'Q3', nodeId: 'N1', queueName: 'Q3', channelType: 'REALTIME' as const, queueType: 'DEST' as const, queueStatus: 'ENABLED' as const, description: null, createTime: '2026-04-20T10:00:00', updateTime: '2026-04-20T10:00:00' },
    ];
    vi.mocked(tlqQueueApi.batchGenerate).mockResolvedValue(response);
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { organizationCode: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.formRef.validate = () => Promise.resolve(true);

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '生成',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.batchGenerate).toHaveBeenCalledTimes(1);
    const payload = vi.mocked(tlqQueueApi.batchGenerate).mock.calls[0]![0];
    expect(payload).toEqual({
      nodeId: 'N1',
      organizationCode: 'A1000143000104',
    });
    expect(ElMessage.success).toHaveBeenCalledWith(expect.stringContaining('已创建 3 条队列'));
    expect(wrapper.emitted('success')).toBeTruthy();
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });

  it('shows info message when response is empty (all standard queues exist)', async () => {
    vi.mocked(tlqQueueApi.batchGenerate).mockResolvedValue([]);
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { organizationCode: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.formRef.validate = () => Promise.resolve(true);

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '生成',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.batchGenerate).toHaveBeenCalledTimes(1);
    expect(ElMessage.info).toHaveBeenCalledWith(
      expect.stringContaining('所有标准队列均已存在'),
    );
    expect(wrapper.emitted('success')).toBeTruthy();
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });

  it('does not call batchGenerate when validation rejects', async () => {
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
    };
    vm.formRef.validate = () =>
      Promise.reject({ organizationCode: [{ message: '机构代码不能为空' }] });

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '生成',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.batchGenerate).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('surfaces ElMessage.error on backend failure and does not emit success', async () => {
    vi.mocked(tlqQueueApi.batchGenerate).mockRejectedValue(
      new Error('服务暂时不可用'),
    );
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      formRef: { validate: () => Promise<boolean> };
    };
    vm.formRef.validate = () => Promise.resolve(true);

    const confirmBtn = Array.from(document.body.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === '生成',
    );
    expect(confirmBtn).toBeTruthy();
    confirmBtn!.click();
    await flushPromises();

    expect(tlqQueueApi.batchGenerate).toHaveBeenCalledTimes(1);
    expect(ElMessage.error).toHaveBeenCalledWith('服务暂时不可用');
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('emits update:modelValue(false) on cancel button click without calling API', async () => {
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
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

    expect(tlqQueueApi.batchGenerate).not.toHaveBeenCalled();
    const closeEvents = wrapper.emitted('update:modelValue');
    expect(closeEvents?.some((e) => e[0] === false)).toBe(true);
  });

  it('restores default organizationCode when dialog re-opens', async () => {
    const wrapper = mount(TlqQueueBatchGenerateDialog, {
      props: { modelValue: true, nodeId: 'N1' },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    let vm = wrapper.vm as unknown as {
      form: { organizationCode: string };
    };
    vm.form.organizationCode = 'CUSTOM_CODE';
    expect(vm.form.organizationCode).toBe('CUSTOM_CODE');

    // Close dialog
    await wrapper.setProps({ modelValue: false });
    await flushPromises();

    // Re-open
    await wrapper.setProps({ modelValue: true });
    await flushPromises();
    vm = wrapper.vm as unknown as {
      form: { organizationCode: string };
    };
    expect(vm.form.organizationCode).toBe('A1000143000104');
  });
});
