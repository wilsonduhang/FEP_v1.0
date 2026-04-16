import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import AuthLetterEditDialog from '../AuthLetterEditDialog.vue';
import { entAuthLetterApi, type AuthLetterResponse } from '../../api/ent-auth-letter-api';

vi.mock('../../api/ent-auth-letter-api');

const draftLetter: AuthLetterResponse = {
  letterId: 'L1',
  enterpriseId: 'E1',
  authType: 'PAPER',
  authScope: 'query',
  authorizedUsci: '91310000MA1K40XK7A',
  authorizedName: 'Acme Corp',
  filePath: null,
  letterStatus: 'DRAFT',
  messageId: null,
  submitTime: null,
  ackTime: null,
  rejectReason: null,
  createTime: '2026-04-15T10:00:00',
  updateTime: '2026-04-15T10:00:00',
};

const acknowledgedLetter: AuthLetterResponse = {
  ...draftLetter,
  letterId: 'L2',
  letterStatus: 'ACKNOWLEDGED',
};

const electronicLetter: AuthLetterResponse = {
  ...draftLetter,
  letterId: 'L3',
  authType: 'ELECTRONIC',
  filePath: '/data/auth/cert.pdf',
};

describe('AuthLetterEditDialog', () => {
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

  it('shows "新建授权书" title when letter is null (create mode)', async () => {
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('新建授权书');
  });

  it('shows "编辑授权书" title and editable form for DRAFT letter', async () => {
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: draftLetter },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('编辑授权书');
    // Save button should exist
    const buttons = document.body.querySelectorAll('button');
    const saveBtn = Array.from(buttons).find((b) => b.textContent?.includes('保存'));
    expect(saveBtn).toBeTruthy();
  });

  it('shows "查看授权书" with no save button for non-DRAFT letter', async () => {
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: acknowledgedLetter },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('查看授权书');
    // No save/create button
    const buttons = document.body.querySelectorAll('button');
    const saveBtn = Array.from(buttons).find(
      (b) => b.textContent?.includes('保存') || b.textContent?.includes('创建'),
    );
    expect(saveBtn).toBeUndefined();
  });

  it('has USCI validation rule rejecting 17-char input', async () => {
    const wrapper = mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as { rules: Record<string, Array<{ pattern?: RegExp }>> };
    const usciRules = vm.rules.authorizedUsci;
    const patternRule = usciRules?.find((r: { pattern?: RegExp }) => r.pattern);
    expect(patternRule?.pattern).toEqual(/^[0-9A-Z]{18}$/);
    // 17-char string should NOT match
    expect(patternRule?.pattern?.test('91310000MA1K40XK7')).toBe(false);
  });

  it('shows filePath field for ELECTRONIC type and hides for PAPER', async () => {
    // ELECTRONIC: filePath visible
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: electronicLetter },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    let body = document.body.textContent || '';
    expect(body).toContain('文件路径');

    // Cleanup
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
    const container2 = document.createElement('div');
    document.body.appendChild(container2);

    // PAPER: filePath hidden
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: draftLetter },
      global: { plugins: [ElementPlus] },
      attachTo: container2,
    });
    await flushPromises();
    body = document.body.textContent || '';
    expect(body).not.toContain('文件路径');
  });

  it('calls create API in create mode', async () => {
    const created: AuthLetterResponse = { ...draftLetter, letterId: 'L99' };
    vi.mocked(entAuthLetterApi.create).mockResolvedValue(created);
    mount(AuthLetterEditDialog, {
      props: { modelValue: true, letter: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();

    // Verify create button exists
    const buttons = document.body.querySelectorAll('button');
    const createBtn = Array.from(buttons).find((b) => b.textContent?.includes('创建'));
    expect(createBtn).toBeTruthy();
  });
});
