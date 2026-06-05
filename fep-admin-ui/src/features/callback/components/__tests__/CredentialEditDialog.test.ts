import { mount, flushPromises } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import CredentialEditDialog from '../CredentialEditDialog.vue';
import { callbackCredentialApi } from '../../api/callbackCredential';
import { findButtonByText, setupElMessageSpies } from '@/shared/test-utils';

vi.mock('../../api/callbackCredential');

describe('CredentialEditDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    setupElMessageSpies();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) document.body.removeChild(document.body.firstChild);
  });

  it('shows create title and calls create on confirm', async () => {
    vi.mocked(callbackCredentialApi.create).mockResolvedValue({} as never);
    const wrapper = mount(CredentialEditDialog, {
      props: { modelValue: true, record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(document.body.textContent || '').toContain('新建凭证');

    const vm = wrapper.vm as unknown as {
      form: { interfaceId: string; authType: string; token: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.interfaceId = 'IF-001';
    vm.form.authType = 'TOKEN';
    vm.form.token = 'plain';
    vm.formRef.validate = () => Promise.resolve(true);

    findButtonByText('确定')!.click();
    await flushPromises();

    expect(callbackCredentialApi.create).toHaveBeenCalledTimes(1);
    const arg = vi.mocked(callbackCredentialApi.create).mock.calls[0][0];
    expect(arg.interfaceId).toBe('IF-001');
    expect(arg.token).toBe('plain');
    expect(ElMessage.success).toHaveBeenCalledWith('已创建');
    expect(wrapper.emitted('saved')).toBeTruthy();
  });

  it('update omits blank secret fields (留空保留原密文)', async () => {
    vi.mocked(callbackCredentialApi.update).mockResolvedValue({} as never);
    const record = {
      interfaceId: 'IF-001',
      authType: 'TOKEN',
      tokenHeader: 'Authorization',
      tokenConfigured: true,
    } as never;
    const wrapper = mount(CredentialEditDialog, {
      props: { modelValue: true, record },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as { formRef: { validate: () => Promise<boolean> } };
    vm.formRef.validate = () => Promise.resolve(true);

    findButtonByText('确定')!.click();
    await flushPromises();

    expect(callbackCredentialApi.update).toHaveBeenCalledWith('IF-001', {
      token: undefined,
      tokenHeader: 'Authorization',
      oauthClientId: undefined,
      oauthClientSecret: undefined,
      oauthTokenEndpoint: undefined,
      oauthScope: undefined,
    });
  });

  it('does not call create when validation rejects (guard on confirm)', async () => {
    // R1 ISSUE-2 guard: when el-form validation fails, onSave must NOT call the API.
    // jsdom cannot run element-plus async-validator reliably, so per the repo convention
    // (see TlqQueueCreateDialog.test.ts) we stub formRef.validate to REJECT — element-plus
    // rejects validate() with invalidFields when a required rule fails. The button is still
    // DOM-driven (findButtonByText().click()), only the validate outcome is simulated
    // (feedback_unit_test_bypass: behavior stays DOM-driven, not vm.method()).
    const wrapper = mount(CredentialEditDialog, {
      props: { modelValue: true, record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      form: { interfaceId: string; authType: string };
      formRef: { validate: () => Promise<boolean> };
    };
    vm.form.interfaceId = 'IF-002';
    vm.form.authType = 'TOKEN';
    // Stub validate to REJECT immediately before the click — no flushPromises in
    // between, or el-form re-exposes its real validate and overwrites the stub.
    vm.formRef.validate = () => Promise.reject({ token: [{ message: '请输入 Token' }] });

    findButtonByText('确定')!.click();
    await flushPromises();

    expect(callbackCredentialApi.create).not.toHaveBeenCalled();
    expect(wrapper.emitted('saved')).toBeUndefined();
  });

  it('rules require the secret on create per authType, optional on edit (R1 ISSUE-2)', async () => {
    // Deterministic assertion of the conditional-required logic itself (env-independent),
    // since jsdom cannot exercise element-plus runtime validation. Real-browser enforcement
    // is covered by the rules wiring + E2E.
    const createWrapper = mount(CredentialEditDialog, {
      props: { modelValue: true, record: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const createVm = createWrapper.vm as unknown as {
      form: { authType: string };
      rules: Record<string, Array<{ required?: boolean }>>;
    };
    // create + TOKEN → token required
    expect(createVm.rules.token?.[0]?.required).toBe(true);
    // create + OAUTH2 → oauth secrets required, token not
    createVm.form.authType = 'OAUTH2';
    await flushPromises();
    expect(createVm.rules.oauthClientId?.[0]?.required).toBe(true);
    expect(createVm.rules.oauthClientSecret?.[0]?.required).toBe(true);
    expect(createVm.rules.oauthTokenEndpoint?.[0]?.required).toBe(true);
    expect(createVm.rules.token).toBeUndefined();

    // edit mode → secrets optional (留空保留原密文)
    const editWrapper = mount(CredentialEditDialog, {
      props: {
        modelValue: true,
        record: { interfaceId: 'IF-001', authType: 'TOKEN', tokenConfigured: true } as never,
      },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const editVm = editWrapper.vm as unknown as { rules: Record<string, unknown> };
    expect(editVm.rules.token).toBeUndefined();
    expect(editVm.rules.interfaceId).toBeDefined();
  });
});
