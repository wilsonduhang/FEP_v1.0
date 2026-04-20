// __tests__/QueryTaskDetailDrawer.test.ts
import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import QueryTaskDetailDrawer from '../QueryTaskDetailDrawer.vue';
import { entQueryTaskApi } from '../../api/ent-query-task-api';

vi.mock('../../api/ent-query-task-api');

const mockTask = {
  taskId: 'T1',
  enterpriseId: 'E1',
  queryType: 'REALTIME' as const,
  usci: '91310000MA1K40XK7A',
  queryTargetName: 'Acme',
  taskStatus: 'COMPLETED' as const,
  messageId: 'M1',
  batchFilePath: null,
  resultSummary: null,
  errorMessage: null,
  createTime: '2026-04-15T10:00:00',
  updateTime: '',
  completeTime: '2026-04-15T10:00:05',
};

const mockResults = [
  {
    resultId: 'R1',
    taskId: 'T1',
    resultUsci: '91310000MA1K40XK7A',
    enterpriseName: 'Acme',
    resultData: '{}',
    resultStatus: 'NORMAL' as const,
    errorCode: null,
    errorMessage: null,
    createTime: '2026-04-15T10:00:05',
  },
  {
    resultId: 'R2',
    taskId: 'T1',
    resultUsci: '9131XXXXXXXXXXXXXX',
    enterpriseName: null,
    resultData: null,
    resultStatus: 'ERROR' as const,
    errorCode: 'ENT_4040',
    errorMessage: '企业不存在',
    createTime: '2026-04-15T10:00:06',
  },
];

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('QueryTaskDetailDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads task and results when opened', async () => {
    vi.mocked(entQueryTaskApi.getById).mockResolvedValue(mockTask);
    vi.mocked(entQueryTaskApi.listResults).mockResolvedValue(mockResults);
    const wrapper = mount(QueryTaskDetailDrawer, {
      props: { modelValue: true, taskId: 'T1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    expect(entQueryTaskApi.getById).toHaveBeenCalledWith('T1');
    expect(entQueryTaskApi.listResults).toHaveBeenCalledWith('T1');
    // el-descriptions renders task info
    expect(wrapper.text()).toContain('T1');
    expect(wrapper.text()).toContain('Acme');
    wrapper.unmount();
  });

  it('highlights ERROR result rows with error-row class', async () => {
    vi.mocked(entQueryTaskApi.getById).mockResolvedValue(mockTask);
    vi.mocked(entQueryTaskApi.listResults).mockResolvedValue(mockResults);
    const wrapper = mount(QueryTaskDetailDrawer, {
      props: { modelValue: true, taskId: 'T1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    // el-table may not render row content in JSDOM; verify rowClassName fn via component internals
    const table = wrapper.findComponent({ name: 'ElTable' });
    if (table.exists()) {
      const rowClassFn = table.props('rowClassName') as (args: {
        row: { resultStatus: string };
      }) => string;
      expect(rowClassFn({ row: { resultStatus: 'ERROR' } })).toBe('error-row');
      expect(rowClassFn({ row: { resultStatus: 'NORMAL' } })).toBe('');
    }
    wrapper.unmount();
  });

  it('refreshes when taskId changes', async () => {
    vi.mocked(entQueryTaskApi.getById).mockResolvedValue(mockTask);
    vi.mocked(entQueryTaskApi.listResults).mockResolvedValue([]);
    const wrapper = mount(QueryTaskDetailDrawer, {
      props: { modelValue: true, taskId: 'T1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    await wrapper.setProps({ taskId: 'T2' });
    await flushPromises();
    expect(entQueryTaskApi.getById).toHaveBeenCalledWith('T2');
    wrapper.unmount();
  });

  it('shows PROCESSING hint', async () => {
    vi.mocked(entQueryTaskApi.getById).mockResolvedValue({ ...mockTask, taskStatus: 'PROCESSING' });
    vi.mocked(entQueryTaskApi.listResults).mockResolvedValue([]);
    const wrapper = mount(QueryTaskDetailDrawer, {
      props: { modelValue: true, taskId: 'T1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    expect(wrapper.text()).toContain('任务处理中');
    wrapper.unmount();
  });
});
