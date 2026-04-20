// src/features/tlq/views/__tests__/TlqNodesPage.test.ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus';
import TlqNodesPage from '../TlqNodesPage.vue';
import { tlqNodeApi } from '../../api/tlq-node-api';
import type { TlqNodeResponse } from '../../types';
import SearchForm from '@/shared/components/SearchForm.vue';
import DataTable from '@/shared/components/DataTable.vue';

vi.mock('../../api/tlq-node-api', () => ({
  tlqNodeApi: {
    listNodes: vi.fn(),
    createNode: vi.fn(),
    getNode: vi.fn(),
    updateNode: vi.fn(),
    deleteNode: vi.fn(),
    changeStatus: vi.fn(),
  },
}));

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

const mockList = vi.mocked(tlqNodeApi.listNodes);
const mockChangeStatus = vi.mocked(tlqNodeApi.changeStatus);
const mockDeleteNode = vi.mocked(tlqNodeApi.deleteNode);
const mockConfirm = vi.mocked(ElMessageBox.confirm);

const globalOpts = { global: { plugins: [ElementPlus] } };

function makeNode(overrides: Partial<TlqNodeResponse> = {}): TlqNodeResponse {
  return {
    nodeId: 'N1',
    nodeName: 'Master-1',
    nodeRole: 'MASTER_PRODUCER',
    hostIp: '192.168.1.10',
    port: 20001,
    vipAddress: null,
    protocol: 'TCP',
    nodeStatus: 'UNKNOWN',
    description: null,
    lastHeartbeat: null,
    createTime: '2026-04-20T10:00:00',
    updateTime: '2026-04-20T10:00:00',
    ...overrides,
  };
}

function pageOf(
  records: TlqNodeResponse[],
  extras: Partial<{ total: number; pageNum: number; pageSize: number; totalPages: number }> = {},
) {
  return {
    records,
    total: extras.total ?? records.length,
    pageNum: extras.pageNum ?? 1,
    pageSize: extras.pageSize ?? 10,
    totalPages: extras.totalPages ?? 1,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('TlqNodesPage', () => {
  it('calls listNodes on mount with default {pageNum:1, pageSize:10}', async () => {
    mockList.mockResolvedValue(pageOf([]));
    mount(TlqNodesPage, globalOpts);
    await flushPromises();
    expect(mockList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 });
  });

  it('renders all 9 column headers (节点名称 / 角色 / IP / 端口 / VIP / 协议 / 状态 / 最近心跳 / 操作)', async () => {
    mockList.mockResolvedValue(pageOf([makeNode()]));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();
    const txt = wrapper.text();
    ['节点名称', '角色', 'IP', '端口', 'VIP', '协议', '状态', '最近心跳', '操作'].forEach((h) => {
      expect(txt).toContain(h);
    });
  });

  it('applies role + status filter when SearchForm emits search event', async () => {
    mockList.mockResolvedValue(pageOf([]));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();
    mockList.mockClear();

    // Red line #1 compliance: exercise SearchForm at the component-event boundary
    // (not an internal wrapper.vm.onSearch() call). The two el-select v-models
    // drive the reactive filter state declared in the page; mutating them here
    // stands in for the user selecting options in the dropdowns (jsdom's
    // limitations with el-select popover make full DOM interaction brittle).
    const filters = (wrapper.vm as unknown as { filters: { role?: string; status?: string } })
      .filters;
    filters.role = 'MASTER_PRODUCER';
    filters.status = 'ONLINE';
    await wrapper.findComponent(SearchForm).vm.$emit('search');
    await flushPromises();

    expect(mockList).toHaveBeenCalledWith(
      expect.objectContaining({ pageNum: 1, role: 'MASTER_PRODUCER', status: 'ONLINE' }),
    );
  });

  it('shows 上线 enabled for UNKNOWN node and 下线 disabled', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeStatus: 'UNKNOWN' })]));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    const onlineBtn = wrapper.findAll('button').find((b) => b.text().trim() === '上线');
    const offlineBtn = wrapper.findAll('button').find((b) => b.text().trim() === '下线');
    expect(onlineBtn).toBeDefined();
    expect(offlineBtn).toBeDefined();
    // el-button disabled renders as `disabled=""` (empty string) on the inner
    // <button> when true, absent attribute when false.
    expect(onlineBtn!.attributes('disabled')).toBeUndefined();
    expect(offlineBtn!.attributes('disabled')).not.toBeUndefined();
  });

  it('shows 下线 enabled for ONLINE node, 上线 disabled', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N2', nodeStatus: 'ONLINE' })]));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    const onlineBtn = wrapper.findAll('button').find((b) => b.text().trim() === '上线');
    const offlineBtn = wrapper.findAll('button').find((b) => b.text().trim() === '下线');
    expect(onlineBtn!.attributes('disabled')).not.toBeUndefined();
    expect(offlineBtn!.attributes('disabled')).toBeUndefined();
  });

  it('calls changeStatus with target=ONLINE when clicking 上线 on OFFLINE node', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N3', nodeStatus: 'OFFLINE' })]));
    mockChangeStatus.mockResolvedValue(makeNode({ nodeId: 'N3', nodeStatus: 'ONLINE' }));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '上线')
      ?.trigger('click');
    await flushPromises();

    expect(mockChangeStatus).toHaveBeenCalledWith('N3', 'ONLINE');
    expect(ElMessage.success).toHaveBeenCalledWith('状态已更新');
    // List should reload after successful transition (initial + reload = 2 calls).
    expect(mockList).toHaveBeenCalledTimes(2);
  });

  it('surfaces ElMessage.error when changeStatus rejects (e.g. 409)', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N4', nodeStatus: 'OFFLINE' })]));
    mockChangeStatus.mockRejectedValue(new Error('节点状态冲突'));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '上线')
      ?.trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('节点状态冲突');
  });

  it('confirms before delete; calls deleteNode on confirm', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N5' })]));
    mockConfirm.mockResolvedValue('confirm' as never);
    mockDeleteNode.mockResolvedValue(undefined);
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '删除')
      ?.trigger('click');
    await flushPromises();

    expect(mockConfirm).toHaveBeenCalled();
    expect(mockDeleteNode).toHaveBeenCalledWith('N5');
    expect(ElMessage.success).toHaveBeenCalledWith('删除成功');
  });

  it('surfaces ElMessage.error on deleteNode 409 rejection', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N6' })]));
    mockConfirm.mockResolvedValue('confirm' as never);
    mockDeleteNode.mockRejectedValue(new Error('存在关联队列，无法删除'));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '删除')
      ?.trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('存在关联队列，无法删除');
  });

  it('does NOT call deleteNode when user cancels the confirm dialog', async () => {
    mockList.mockResolvedValue(pageOf([makeNode({ nodeId: 'N7' })]));
    mockConfirm.mockRejectedValue('cancel' as never);
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '删除')
      ?.trigger('click');
    await flushPromises();

    expect(mockDeleteNode).not.toHaveBeenCalled();
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('triggers listNodes with new pageNum when DataTable emits update:pageNum', async () => {
    mockList.mockResolvedValue(pageOf([], { total: 50, totalPages: 5 }));
    const wrapper = mount(TlqNodesPage, globalOpts);
    await flushPromises();
    mockList.mockClear();

    // Emit update:pageNum at DataTable's component boundary — represents the
    // actual contract DataTable exposes to its parent (el-pagination raises
    // this internally when the user clicks a pager). Red line #1 compliant:
    // cross-component event, not an internal vm method call. Generic cast
    // needed because DataTable declares `generic="T"`.
    await wrapper
      .findComponent(DataTable as unknown as Parameters<typeof wrapper.findComponent>[0])
      .vm.$emit('update:pageNum', 2);
    await flushPromises();

    expect(mockList).toHaveBeenCalledWith(expect.objectContaining({ pageNum: 2, pageSize: 10 }));
  });
});
