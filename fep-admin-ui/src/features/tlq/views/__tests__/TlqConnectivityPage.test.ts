// src/features/tlq/views/__tests__/TlqConnectivityPage.test.ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import TlqConnectivityPage from '../TlqConnectivityPage.vue';
import { tlqNodeApi } from '../../api/tlq-node-api';
import { tlqConnectivityApi } from '../../api/tlq-connectivity-api';
import type {
  ConnectivityRecordResponse,
  ConnectivitySummaryResponse,
  ConnectivityTestResponse,
  TlqNodeResponse,
} from '../../types';

vi.mock('../../api/tlq-node-api', () => ({
  tlqNodeApi: {
    listNodes: vi.fn(),
  },
}));

vi.mock('../../api/tlq-connectivity-api', () => ({
  tlqConnectivityApi: {
    triggerTest: vi.fn(),
    listRecords: vi.fn(),
    getSummary: vi.fn(),
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
  };
});

const mockListNodes = vi.mocked(tlqNodeApi.listNodes);
const mockTrigger = vi.mocked(tlqConnectivityApi.triggerTest);
const mockListRecords = vi.mocked(tlqConnectivityApi.listRecords);
const mockGetSummary = vi.mocked(tlqConnectivityApi.getSummary);

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

function makeSummary(
  overrides: Partial<ConnectivitySummaryResponse> = {},
): ConnectivitySummaryResponse {
  return {
    nodeId: 'N1',
    lastResult: 'SUCCESS',
    lastTestTime: '2026-04-20T10:00:00',
    totalTests: 10,
    successCount: 7,
    successRate: 70,
    ...overrides,
  };
}

function makeRecord(
  overrides: Partial<ConnectivityRecordResponse> = {},
): ConnectivityRecordResponse {
  return {
    recordId: 'R1',
    nodeId: 'N1',
    testTime: '2026-04-20T10:00:00',
    testResult: 'SUCCESS',
    rttMs: 12,
    errorMessage: null,
    triggeredBy: 'admin',
    ...overrides,
  };
}

function pageOfNodes(records: TlqNodeResponse[]) {
  return {
    records,
    total: records.length,
    pageNum: 1,
    pageSize: 1000,
    totalPages: 1,
  };
}

function pageOfRecords(records: ConnectivityRecordResponse[]) {
  return {
    records,
    total: records.length,
    pageNum: 1,
    pageSize: 10,
    totalPages: 1,
  };
}

function makeTriggerResp(
  overrides: Partial<ConnectivityTestResponse> = {},
): ConnectivityTestResponse {
  return {
    recordId: 'R1',
    nodeId: 'N1',
    result: 'SUCCESS',
    rttMs: 15,
    message: '心跳成功',
    testTime: '2026-04-20T10:00:00',
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('TlqConnectivityPage', () => {
  it('loads node list on mount via listNodes({pageNum:1, pageSize:1000})', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode()]));
    mount(TlqConnectivityPage, globalOpts);
    await flushPromises();
    expect(mockListNodes).toHaveBeenCalledWith({ pageNum: 1, pageSize: 1000 });
  });

  it('shows empty state when no node selected; does not call connectivity APIs', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode()]));
    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    expect(wrapper.text()).toContain('请先选择节点');
    expect(mockGetSummary).not.toHaveBeenCalled();
    expect(mockListRecords).not.toHaveBeenCalled();
  });

  it('loads summary + records in parallel on node select', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(makeSummary());
    mockListRecords.mockResolvedValue(pageOfRecords([makeRecord()]));

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    // Red line #1 compliance: set selectedNodeId via exposed ref (simulating
    // el-select option pick) then emit change on the ElSelect child to run the
    // boundary handler. No internal vm.loadData() / vm.trigger() direct calls.
    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();

    // Find ElSelect component and fire its 'change' event with the new value.
    const select = wrapper.findComponent({ name: 'ElSelect' });
    expect(select.exists()).toBe(true);
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    expect(mockGetSummary).toHaveBeenCalledWith('N1');
    expect(mockListRecords).toHaveBeenCalledWith('N1', { pageNum: 1, pageSize: 10 });
  });

  it('displays successRate as "-" when totalTests === 0 (NaN guard)', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(
      makeSummary({ totalTests: 0, successCount: 0, successRate: 0 }),
    );
    mockListRecords.mockResolvedValue(pageOfRecords([]));

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    // successRate card must show "-" (not NaN%).
    const text = wrapper.text();
    expect(text).not.toContain('NaN');
    // The 成功率 card body should render "-".
    const rateCard = wrapper
      .findAll('.stat-card')
      .find((c) => c.text().includes('成功率'));
    expect(rateCard).toBeDefined();
    expect(rateCard!.text()).toContain('-');
  });

  it('displays successRate formatted to 2 decimals when totalTests > 0', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(
      makeSummary({ totalTests: 10, successCount: 7, successRate: 70 }),
    );
    mockListRecords.mockResolvedValue(pageOfRecords([]));

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    expect(wrapper.text()).toContain('70.00%');
  });

  it('calls ElMessage.warning on TIMEOUT trigger result and refreshes data', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(makeSummary());
    mockListRecords.mockResolvedValue(pageOfRecords([makeRecord()]));
    mockTrigger.mockResolvedValue(
      makeTriggerResp({ result: 'TIMEOUT', rttMs: null, message: '心跳超时' }),
    );

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    // Clear counters to isolate the refresh calls after trigger.
    mockGetSummary.mockClear();
    mockListRecords.mockClear();

    // Click the trigger button (DOM click — red line #1 compliant).
    const triggerBtn = wrapper
      .findAll('button')
      .find((b) => b.text().trim().includes('触发 9005 心跳测试'));
    expect(triggerBtn).toBeDefined();
    await triggerBtn!.trigger('click');
    await flushPromises();

    expect(mockTrigger).toHaveBeenCalledWith('N1');
    expect(ElMessage.warning).toHaveBeenCalled();
    // Data must be refreshed after trigger.
    expect(mockGetSummary).toHaveBeenCalledWith('N1');
    expect(mockListRecords).toHaveBeenCalledWith('N1', { pageNum: 1, pageSize: 10 });
  });

  it('dispatches ElMessage.success with RTT appended on SUCCESS trigger result', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(makeSummary());
    mockListRecords.mockResolvedValue(pageOfRecords([makeRecord()]));
    mockTrigger.mockResolvedValue(
      makeTriggerResp({ result: 'SUCCESS', rttMs: 18, message: '心跳成功' }),
    );

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim().includes('触发 9005 心跳测试'))
      ?.trigger('click');
    await flushPromises();

    expect(ElMessage.success).toHaveBeenCalledWith('心跳成功 (18ms)');
  });

  it('dispatches ElMessage.error on FAILURE trigger result', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(makeSummary());
    mockListRecords.mockResolvedValue(pageOfRecords([makeRecord()]));
    mockTrigger.mockResolvedValue(
      makeTriggerResp({ result: 'FAILURE', rttMs: null, message: '连接被拒绝' }),
    );

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    await wrapper
      .findAll('button')
      .find((b) => b.text().trim().includes('触发 9005 心跳测试'))
      ?.trigger('click');
    await flushPromises();

    expect(ElMessage.error).toHaveBeenCalledWith('连接被拒绝');
  });

  it('renders MockBadge near the trigger button', async () => {
    mockListNodes.mockResolvedValue(pageOfNodes([makeNode({ nodeId: 'N1' })]));
    mockGetSummary.mockResolvedValue(makeSummary());
    mockListRecords.mockResolvedValue(pageOfRecords([]));

    const wrapper = mount(TlqConnectivityPage, globalOpts);
    await flushPromises();

    const vm = wrapper.vm as unknown as { selectedNodeId: string };
    vm.selectedNodeId = 'N1';
    await flushPromises();
    const select = wrapper.findComponent({ name: 'ElSelect' });
    await select.vm.$emit('change', 'N1');
    await flushPromises();

    expect(wrapper.find('[data-test="mock-badge"]').exists()).toBe(true);
  });
});
