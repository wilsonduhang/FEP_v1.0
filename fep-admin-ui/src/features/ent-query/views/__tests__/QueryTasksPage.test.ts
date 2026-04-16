import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import QueryTasksPage from '../QueryTasksPage.vue';
import { entQueryTaskApi, type QueryTaskResponse } from '../../api/ent-query-task-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/ent-query-task-api');

const mockPage: PageResult<QueryTaskResponse> = { records: [{
  taskId: 'T1', enterpriseId: 'E1', queryType: 'REALTIME', usci: '91310000MA1K40XK7A',
  queryTargetName: 'Acme', taskStatus: 'DRAFT', messageId: null, batchFilePath: null,
  resultSummary: null, errorMessage: null, createTime: '2026-04-15T10:00:00', updateTime: '',
  completeTime: null,
}], total: 1, pageNum: 1, pageSize: 20, totalPages: 1 };

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div/>' } }],
});

const globalPlugins = { global: { plugins: [router, ElementPlus] } };

describe('QueryTasksPage', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('loads tasks on mount', async () => {
    vi.mocked(entQueryTaskApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(QueryTasksPage, globalPlugins);
    await flushPromises();
    expect(entQueryTaskApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('T1');
  });

  it('displays Mock Mode badge', async () => {
    vi.mocked(entQueryTaskApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(QueryTasksPage, globalPlugins);
    await flushPromises();
    expect(wrapper.text()).toContain('TLQ Mock 模式');
  });

  it('calls execute api when triggered on DRAFT task', async () => {
    vi.mocked(entQueryTaskApi.search).mockResolvedValue(mockPage);
    vi.mocked(entQueryTaskApi.execute).mockResolvedValue(mockPage.records[0]);
    const wrapper = mount(QueryTasksPage, globalPlugins);
    await flushPromises();
    // Find execute button by text
    await wrapper.findAll('button').find((b) => b.text() === '执行')?.trigger('click');
    await flushPromises();
    expect(entQueryTaskApi.execute).toHaveBeenCalledWith('T1');
  });

  it('triggers search on onSearch event', async () => {
    vi.mocked(entQueryTaskApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(QueryTasksPage, globalPlugins);
    await flushPromises();
    // search button inside SearchForm
    await wrapper.findAll('button').find((b) => b.text() === '搜索')?.trigger('click');
    expect(entQueryTaskApi.search).toHaveBeenCalledTimes(2);
  });

  it('resets page number on page size change', async () => {
    vi.mocked(entQueryTaskApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(QueryTasksPage, globalPlugins);
    await flushPromises();
    const table = wrapper.findComponent({ name: 'DataTable' });
    table.vm.$emit('update:pageSize', 50);
    await flushPromises();
    expect(vi.mocked(entQueryTaskApi.search).mock.lastCall?.[0]).toMatchObject({ pageNum: 1, pageSize: 50 });
  });
});
