import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus, { ElMessage } from 'element-plus';
import OutputInterfacesPage from '../OutputInterfacesPage.vue';
import {
  subOutputInterfaceApi,
  type OutputInterfaceResponse,
} from '../../api/sub-output-interface-api';
import type { PageResult } from '@/shared/types/page-result';

vi.mock('../../api/sub-output-interface-api');
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

const mockRecord: OutputInterfaceResponse = {
  interfaceId: 'I-1',
  interfaceName: '对账回传',
  interfaceUrl: 'https://bank.example.com/recon',
  businessTypeId: '3000',
  authType: 'TOKEN',
  timeoutSeconds: 60,
  retryCount: 2,
  interfaceStatus: 'ENABLED',
  lastCallTime: null,
  callCount: 5,
  createTime: '2026-04-17T10:00:00',
  updateTime: '2026-04-17T10:00:00',
};

const mockPage: PageResult<OutputInterfaceResponse> = {
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

describe('OutputInterfacesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the output interface list on mount', async () => {
    vi.mocked(subOutputInterfaceApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(OutputInterfacesPage, globalPlugins);
    await flushPromises();
    expect(subOutputInterfaceApi.search).toHaveBeenCalled();
    expect(wrapper.text()).toContain('对账回传');
    expect(wrapper.text()).toContain('https://bank.example.com/recon');
  });

  it('updates local row after toggleStatus (via button click)', async () => {
    vi.mocked(subOutputInterfaceApi.search).mockResolvedValue(mockPage);
    const toggled: OutputInterfaceResponse = { ...mockRecord, interfaceStatus: 'DISABLED' };
    vi.mocked(subOutputInterfaceApi.toggleStatus).mockResolvedValue(toggled);

    const wrapper = mount(OutputInterfacesPage, globalPlugins);
    await flushPromises();

    // Find toggle button by its text (current state is ENABLED → button shows "停用").
    // Aligns with feedback_unit_test_bypass red-line: exercise the real framework
    // dispatch path instead of calling `vm.onToggleStatus` directly.
    const toggleButton = wrapper
      .findAll('button')
      .find((b) => b.text().includes('停用'));
    expect(toggleButton, 'toggle button must exist').toBeDefined();

    await toggleButton!.trigger('click');
    await flushPromises();

    expect(subOutputInterfaceApi.toggleStatus).toHaveBeenCalledWith('I-1');
    // Should NOT have re-fetched the whole list (AC #2).
    expect(subOutputInterfaceApi.search).toHaveBeenCalledTimes(1);
    // Local row should reflect new status. DOM-side sanity: the localized
    // "禁用" tag is rendered (StatusTag maps DISABLED → "禁用"), proving the
    // click dispatch travelled through the framework and updated the row.
    expect(wrapper.html()).toContain('禁用');
    // Per AC #4, keep the raw enum assertion on the exposed page state — this
    // reads published state, not a method call, so the red-line
    // (feedback_unit_test_bypass) is not violated.
    const state = (wrapper.vm as unknown as { page: { records: OutputInterfaceResponse[] } }).page;
    expect(state.records[0].interfaceStatus).toBe('DISABLED');
  });

  it('shows success ElMessage when connectivity test returns true', async () => {
    vi.mocked(subOutputInterfaceApi.search).mockResolvedValue(mockPage);
    vi.mocked(subOutputInterfaceApi.test).mockResolvedValue(true);
    const wrapper = mount(OutputInterfacesPage, globalPlugins);
    await flushPromises();
    await (wrapper.vm as unknown as { onTest: (id: string) => Promise<void> }).onTest('I-1');
    await flushPromises();
    expect(subOutputInterfaceApi.test).toHaveBeenCalledWith('I-1');
    expect(ElMessage.success).toHaveBeenCalledWith('测试通过');
  });

  it('shows error ElMessage when connectivity test returns false', async () => {
    vi.mocked(subOutputInterfaceApi.search).mockResolvedValue(mockPage);
    vi.mocked(subOutputInterfaceApi.test).mockResolvedValue(false);
    const wrapper = mount(OutputInterfacesPage, globalPlugins);
    await flushPromises();
    await (wrapper.vm as unknown as { onTest: (id: string) => Promise<void> }).onTest('I-1');
    await flushPromises();
    expect(ElMessage.error).toHaveBeenCalledWith('测试失败');
  });

  it('renders MockBadge next to the connectivity test button', async () => {
    vi.mocked(subOutputInterfaceApi.search).mockResolvedValue(mockPage);
    const wrapper = mount(OutputInterfacesPage, globalPlugins);
    await flushPromises();
    const mockBadges = wrapper.findAllComponents({ name: 'MockBadge' });
    expect(mockBadges.length).toBeGreaterThan(0);
    // Sanity check: the badge carries the default "Mock 模式" slot content.
    expect(wrapper.text()).toContain('当前 Mock');
  });
});
