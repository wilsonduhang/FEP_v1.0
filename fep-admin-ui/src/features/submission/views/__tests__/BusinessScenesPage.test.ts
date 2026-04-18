import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import BusinessScenesPage from '../BusinessScenesPage.vue';
import { subBusinessSceneApi, type BusinessSceneResponse } from '../../api/sub-business-scene-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/sub-business-scene-api');
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
      confirm: vi.fn().mockResolvedValue(true),
    },
  };
});

const mockRecord: BusinessSceneResponse = {
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

const mockPage: PageResult<BusinessSceneResponse> = {
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

describe('BusinessScenesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the business scene list on mount', async () => {
    vi.mocked(subBusinessSceneApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(BusinessScenesPage, globalPlugins);
    await flushPromises();
    expect(subBusinessSceneApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('对账手动推送');
    expect(wrapper.text()).toContain('https://bank.example.com/recon');
  });

  it('updates local row after toggleStatus (no full list re-fetch)', async () => {
    vi.mocked(subBusinessSceneApi.search).mockResolvedValue(mockPage);
    const toggled: BusinessSceneResponse = { ...mockRecord, sceneStatus: 'DISABLED' };
    vi.mocked(subBusinessSceneApi.toggleStatus).mockResolvedValue(toggled);
    const wrapper = mount(BusinessScenesPage, globalPlugins);
    await flushPromises();
    await (
      wrapper.vm as unknown as { onToggleStatus: (r: BusinessSceneResponse) => Promise<void> }
    ).onToggleStatus(mockRecord);
    await flushPromises();
    expect(subBusinessSceneApi.toggleStatus).toHaveBeenCalledWith('SC-1');
    // AC #2: no full re-fetch — search should only run once (the initial mount).
    expect(subBusinessSceneApi.search).toHaveBeenCalledTimes(1);
    const state = (wrapper.vm as unknown as { page: { records: BusinessSceneResponse[] } }).page;
    expect(state.records[0].sceneStatus).toBe('DISABLED');
  });

  it('opens dialog in create mode when 新建 is clicked', async () => {
    vi.mocked(subBusinessSceneApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(BusinessScenesPage, globalPlugins);
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      openCreate: () => void;
      dialogVisible: boolean;
      dialogMode: 'create' | 'edit';
      editingRecord: BusinessSceneResponse | null;
    };
    vm.openCreate();
    await flushPromises();
    expect(vm.dialogVisible).toBe(true);
    expect(vm.dialogMode).toBe('create');
    expect(vm.editingRecord).toBeNull();
  });
});
