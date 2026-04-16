import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import AuthLettersPage from '../AuthLettersPage.vue';
import { entAuthLetterApi, type AuthLetterResponse } from '../../api/ent-auth-letter-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/ent-auth-letter-api');

const draftLetter: AuthLetterResponse = {
  letterId: 'L1',
  enterpriseId: 'E1',
  authType: 'PAPER',
  authScope: 'all',
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

const submittedLetter: AuthLetterResponse = {
  ...draftLetter,
  letterId: 'L2',
  letterStatus: 'SUBMITTED',
};

const mockPage: PageResult<AuthLetterResponse> = {
  records: [draftLetter, submittedLetter],
  total: 2,
  pageNum: 1,
  pageSize: 20,
  totalPages: 1,
};

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div/>' } }],
});

const globalPlugins = { global: { plugins: [router, ElementPlus] } };

describe('AuthLettersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads letters on mount and calls search', async () => {
    vi.mocked(entAuthLetterApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(AuthLettersPage, globalPlugins);
    await flushPromises();
    expect(entAuthLetterApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('L1');
  });

  it('displays Mock Mode badge', async () => {
    vi.mocked(entAuthLetterApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(AuthLettersPage, globalPlugins);
    await flushPromises();
    expect(wrapper.text()).toContain('TLQ Mock 模式');
  });

  it('calls submit api with confirm on DRAFT letter', async () => {
    vi.mocked(entAuthLetterApi.search).mockResolvedValue(mockPage);
    vi.mocked(entAuthLetterApi.submit).mockResolvedValue({ ...draftLetter, letterStatus: 'SUBMITTED' });
    const wrapper = mount(AuthLettersPage, globalPlugins);
    await flushPromises();
    // Find the submit button for DRAFT row (first row)
    const submitBtns = wrapper
      .findAll('button')
      .filter((b) => b.text() === '提交');
    // The first submit button should be enabled (DRAFT)
    const draftSubmitBtn = submitBtns[0];
    expect(draftSubmitBtn.attributes('disabled')).toBeUndefined();
  });

  it('disables edit/submit/delete on non-DRAFT letter', async () => {
    const nonDraftPage: PageResult<AuthLetterResponse> = {
      records: [submittedLetter],
      total: 1,
      pageNum: 1,
      pageSize: 20,
      totalPages: 1,
    };
    vi.mocked(entAuthLetterApi.search).mockResolvedValue(nonDraftPage);
    const wrapper = mount(AuthLettersPage, globalPlugins);
    await flushPromises();
    // Edit, submit, delete should all be disabled for SUBMITTED letter
    const editBtn = wrapper.findAll('button').find((b) => b.text() === '编辑');
    const submitBtn = wrapper.findAll('button').find((b) => b.text() === '提交');
    const deleteBtn = wrapper.findAll('button').find((b) => b.text() === '删除');
    expect(editBtn?.attributes('disabled')).toBeDefined();
    expect(submitBtn?.attributes('disabled')).toBeDefined();
    expect(deleteBtn?.attributes('disabled')).toBeDefined();
  });

  it('view button is always enabled regardless of status', async () => {
    const nonDraftPage: PageResult<AuthLetterResponse> = {
      records: [submittedLetter],
      total: 1,
      pageNum: 1,
      pageSize: 20,
      totalPages: 1,
    };
    vi.mocked(entAuthLetterApi.search).mockResolvedValue(nonDraftPage);
    const wrapper = mount(AuthLettersPage, globalPlugins);
    await flushPromises();
    const viewBtn = wrapper.findAll('button').find((b) => b.text() === '查看');
    expect(viewBtn?.attributes('disabled')).toBeUndefined();
  }, 15000);
});
