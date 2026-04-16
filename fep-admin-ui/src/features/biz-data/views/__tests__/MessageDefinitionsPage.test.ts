import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import MessageDefinitionsPage from '../MessageDefinitionsPage.vue';
import {
  bizMessageDefinitionApi,
  type DefinitionResponse,
} from '../../api/biz-message-definition-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/biz-message-definition-api');

const mockDef: DefinitionResponse = {
  definitionId: 'D1',
  messageCode: '1001',
  messageName: '企业信息查询请求',
  businessTypeId: '3200',
  direction: 'OUTBOUND',
  fieldCount: 12,
  fieldSummary: null,
  sampleXml: null,
  definitionStatus: 'ENABLED',
  sortOrder: 1,
  createTime: '2026-04-15T10:00:00',
  updateTime: '2026-04-15T10:00:00',
};

const mockPage: PageResult<DefinitionResponse> = {
  records: [mockDef],
  total: 1,
  pageNum: 1,
  pageSize: 20,
  totalPages: 1,
};

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div/>' } }],
});

const globalPlugins = { global: { plugins: [router, ElementPlus] } };

describe('MessageDefinitionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads definitions on mount', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    expect(bizMessageDefinitionApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('1001');
    expect(wrapper.text()).toContain('企业信息查询请求');
  });

  it('triggers search on search button click', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '搜索')
      ?.trigger('click');
    expect(bizMessageDefinitionApi.search).toHaveBeenCalledTimes(2);
  });

  it('opens create dialog on button click', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('新建报文定义'))
      ?.trigger('click');
    await flushPromises();
    const dialog = wrapper.findComponent({ name: 'DefinitionEditDialog' });
    expect(dialog.exists()).toBe(true);
  });

  it('calls toggleStatus on toggle button click', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue(mockPage);
    vi.mocked(bizMessageDefinitionApi.toggleStatus).mockResolvedValue(mockDef);
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    // For ENABLED status, button should say '禁用'
    const toggleBtn = wrapper
      .findAll('button')
      .find((b) => b.text() === '禁用');
    expect(toggleBtn).toBeTruthy();
  });

  it('shows enable text for DISABLED definitions', async () => {
    const disabledDef = { ...mockDef, definitionStatus: 'DISABLED' as const };
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue({
      ...mockPage,
      records: [disabledDef],
    });
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    const enableBtn = wrapper
      .findAll('button')
      .find((b) => b.text() === '启用');
    expect(enableBtn).toBeTruthy();
  });

  it('has delete button in operation column', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(MessageDefinitionsPage, globalPlugins);
    await flushPromises();
    const deleteBtn = wrapper
      .findAll('button')
      .find((b) => b.text() === '删除');
    expect(deleteBtn).toBeTruthy();
  });
});
