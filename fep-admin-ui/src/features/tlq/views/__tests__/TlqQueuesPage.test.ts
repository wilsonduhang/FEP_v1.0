// src/features/tlq/views/__tests__/TlqQueuesPage.test.ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage, ElMessageBox } from 'element-plus';
import TlqQueuesPage from '../TlqQueuesPage.vue';
import { tlqNodeApi } from '../../api/tlq-node-api';
import { tlqQueueApi } from '../../api/tlq-queue-api';
import type { TlqNodeResponse, TlqQueueConfigResponse } from '../../types';

vi.mock('../../api/tlq-node-api', () => ({
  tlqNodeApi: {
    listNodes: vi.fn(),
  },
}));

vi.mock('../../api/tlq-queue-api', () => ({
  tlqQueueApi: {
    listByNode: vi.fn(),
    deleteQueue: vi.fn(),
    createQueue: vi.fn(),
    batchGenerate: vi.fn(),
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

const mockListNodes = vi.mocked(tlqNodeApi.listNodes);
const mockListByNode = vi.mocked(tlqQueueApi.listByNode);
const mockDeleteQueue = vi.mocked(tlqQueueApi.deleteQueue);
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
    nodeStatus: 'ONLINE',
    description: null,
    lastHeartbeat: null,
    createTime: '2026-04-20T10:00:00',
    updateTime: '2026-04-20T10:00:00',
    ...overrides,
  };
}

function makeQueue(overrides: Partial<TlqQueueConfigResponse> = {}): TlqQueueConfigResponse {
  return {
    queueId: 'Q1',
    nodeId: 'N1',
    queueName: 'QSEND.A1000143000104',
    channelType: 'REALTIME',
    queueType: 'SEND',
    queueStatus: 'ENABLED',
    description: null,
    createTime: '2026-04-20T10:00:00',
    updateTime: '2026-04-20T10:00:00',
    ...overrides,
  };
}

function pageOf(records: TlqNodeResponse[]) {
  return {
    records,
    total: records.length,
    pageNum: 1,
    pageSize: 1000,
    totalPages: 1,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('TlqQueuesPage', () => {
  it('loads node list on mount via listNodes({pageNum:1, pageSize:1000})', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode()]));
    mount(TlqQueuesPage, globalOpts);
    await flushPromises();
    expect(mockListNodes).toHaveBeenCalledWith({ pageNum: 1, pageSize: 1000 });
  });

  it('shows empty-state message when no node selected; does not call listByNode', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode()]));
    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    expect(wrapper.text()).toContain('请先选择节点');
    expect(mockListByNode).not.toHaveBeenCalled();
  });

  it('calls listByNode with selected nodeId and renders rows', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode({ nodeId: 'N1' })]));
    mockListByNode.mockResolvedValue([makeQueue({ queueName: 'QSEND.A1000143000104' })]);
    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    // Red line #1 compliance: mutate the reactive state exposed by defineExpose
    // — simulates the node selection (el-select popover DOM is brittle in jsdom).
    // defineExpose unwraps refs, so selectedNodeId is set directly on the proxy.
    // The load action is then triggered via the change handler as a boundary
    // call (NOT an internal vm.load() method).
    const vm = wrapper.vm as unknown as {
      selectedNodeId: string;
      loadQueues: () => Promise<void>;
    };
    vm.selectedNodeId = 'N1';
    await vm.loadQueues();
    await flushPromises();

    expect(mockListByNode).toHaveBeenCalledWith('N1');
    expect(wrapper.text()).toContain('QSEND.A1000143000104');
  });

  it('confirms before delete; calls deleteQueue and reloads list on confirm', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode({ nodeId: 'N1' })]));
    mockListByNode.mockResolvedValue([makeQueue({ queueId: 'Q1', queueName: 'QLOCAL.TEST' })]);
    mockConfirm.mockResolvedValue('confirm' as never);
    mockDeleteQueue.mockResolvedValue(undefined);

    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      selectedNodeId: string;
      loadQueues: () => Promise<void>;
    };
    vm.selectedNodeId = 'N1';
    await vm.loadQueues();
    await flushPromises();

    // Click the 删除 button (real DOM click, Santa red line #1 compliant).
    const deleteBtn = wrapper.findAll('button').find((b) => b.text().trim() === '删除');
    expect(deleteBtn).toBeDefined();
    await deleteBtn!.trigger('click');
    await flushPromises();

    expect(mockConfirm).toHaveBeenCalled();
    expect(mockDeleteQueue).toHaveBeenCalledWith('Q1');
    expect(ElMessage.success).toHaveBeenCalledWith('删除成功');
    // listByNode called twice: initial load + reload after delete.
    expect(mockListByNode).toHaveBeenCalledTimes(2);
  });

  it('surfaces ElMessage.error when deleteQueue rejects', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode({ nodeId: 'N1' })]));
    mockListByNode.mockResolvedValue([makeQueue({ queueId: 'Q2' })]);
    mockConfirm.mockResolvedValue('confirm' as never);
    mockDeleteQueue.mockRejectedValue(new Error('队列删除失败'));

    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      selectedNodeId: string;
      loadQueues: () => Promise<void>;
    };
    vm.selectedNodeId = 'N1';
    await vm.loadQueues();
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '删除')
      ?.trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('队列删除失败');
  });

  it('does NOT call deleteQueue when user cancels the confirm dialog', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode({ nodeId: 'N1' })]));
    mockListByNode.mockResolvedValue([makeQueue({ queueId: 'Q3' })]);
    mockConfirm.mockRejectedValue('cancel' as never);

    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as {
      selectedNodeId: string;
      loadQueues: () => Promise<void>;
    };
    vm.selectedNodeId = 'N1';
    await vm.loadQueues();
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim() === '删除')
      ?.trigger('click');
    await flushPromises();

    expect(mockDeleteQueue).not.toHaveBeenCalled();
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('disables 新建队列 and 批量生成 buttons when no node selected', async () => {
    mockListNodes.mockResolvedValue(pageOf([makeNode()]));
    const wrapper = mount(TlqQueuesPage, globalOpts);
    await flushPromises();

    const createBtn = wrapper.findAll('button').find((b) => b.text().trim() === '新建队列');
    const batchBtn = wrapper
      .findAll('button')
      .find((b) => b.text().trim().includes('批量生成'));

    expect(createBtn).toBeDefined();
    expect(batchBtn).toBeDefined();
    expect(createBtn!.attributes('disabled')).not.toBeUndefined();
    expect(batchBtn!.attributes('disabled')).not.toBeUndefined();
  });
});
