import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus, { ElMessageBox } from 'element-plus';
import DataSourcesPage from '../DataSourcesPage.vue';
import {
  subDataSourceApi,
  type DataSourceResponse,
} from '../../api/sub-data-source-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/sub-data-source-api');
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus');
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
    },
  };
});

const mockRecord: DataSourceResponse = {
  sourceId: 'S-1',
  sourceName: '核心系统',
  logoPath: '/logo/core.png',
  contactAddress: '北京市朝阳区建国路 88 号',
  contactPhone: '13800138000',
  pushEnabled: true,
  contentType: 'application/json',
  clientId: 'client-001',
  sourceStatus: 'ENABLED',
  createTime: '2026-04-17T10:00:00',
  updateTime: '2026-04-17T10:00:00',
};

const mockPage: PageResult<DataSourceResponse> = {
  records: [mockRecord],
  total: 1,
  pageNum: 1,
  pageSize: 10,
  totalPages: 1,
};

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div/>' } }],
});

const globalPlugins = { global: { plugins: [router, ElementPlus] } };

describe('DataSourcesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the data source list on mount', async () => {
    vi.mocked(subDataSourceApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(DataSourcesPage, globalPlugins);
    await flushPromises();
    expect(subDataSourceApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('核心系统');
    expect(wrapper.text()).toContain('13800138000');
  });

  it('delete confirms then calls API; cancel does NOT call API', async () => {
    vi.mocked(subDataSourceApi.search).mockResolvedValue(mockPage);
    vi.mocked(subDataSourceApi.remove).mockResolvedValue(undefined);
    const wrapper = mount(DataSourcesPage, globalPlugins);
    await flushPromises();

    // Confirm branch — ElMessageBox.confirm's declared return type is a union
    // (`MessageBoxData`); the simple string literal satisfies the union member
    // that is a bare string. Cast to the union so the narrower mock value
    // type-checks.
    vi.mocked(ElMessageBox.confirm).mockResolvedValueOnce(
      'confirm' as Awaited<ReturnType<typeof ElMessageBox.confirm>>,
    );
    await (wrapper.vm as unknown as { onDelete: (r: DataSourceResponse) => Promise<void> })
      .onDelete(mockRecord);
    await flushPromises();
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(1);
    expect(subDataSourceApi.remove).toHaveBeenCalledWith('S-1');

    // Cancel branch.
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel');
    await (wrapper.vm as unknown as { onDelete: (r: DataSourceResponse) => Promise<void> })
      .onDelete(mockRecord);
    await flushPromises();
    expect(ElMessageBox.confirm).toHaveBeenCalledTimes(2);
    // remove still only called once (confirm branch above), not twice.
    expect(subDataSourceApi.remove).toHaveBeenCalledTimes(1);
  });

  it('opens dialog in create mode when 新建 is clicked', async () => {
    vi.mocked(subDataSourceApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(DataSourcesPage, globalPlugins);
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      openCreate: () => void;
      dialogVisible: boolean;
      dialogMode: 'create' | 'edit';
      editingRecord: DataSourceResponse | null;
    };
    vm.openCreate();
    await flushPromises();
    expect(vm.dialogVisible).toBe(true);
    expect(vm.dialogMode).toBe('create');
    expect(vm.editingRecord).toBeNull();
  });
});
