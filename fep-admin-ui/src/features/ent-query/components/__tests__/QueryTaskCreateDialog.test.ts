import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import QueryTaskCreateDialog from '../QueryTaskCreateDialog.vue';
import { entQueryTaskApi } from '../../api/ent-query-task-api';

vi.mock('../../api/ent-query-task-api');

const mockCreated = {
  taskId: 'T99', enterpriseId: 'E1', queryType: 'REALTIME' as const,
  usci: '91310000MA1K40XK7A', queryTargetName: null, taskStatus: 'DRAFT' as const,
  messageId: null, batchFilePath: null, resultSummary: null, errorMessage: null,
  createTime: '2026-04-15T10:00:00', updateTime: '', completeTime: null,
};

describe('QueryTaskCreateDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    // Clean up teleported dialog elements from jsdom
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
  });

  it('renders dialog with form fields when visible', async () => {
    mount(QueryTaskCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('新建查询任务');
    expect(body).toContain('企业 ID');
    expect(body).toContain('被查询 USCI');
  });

  it('does not show batch file path when REALTIME is selected', async () => {
    mount(QueryTaskCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).not.toContain('批量文件路径');
  });

  it('has USCI validation rule with correct 18-char alphanumeric pattern', async () => {
    const wrapper = mount(QueryTaskCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as { rules: Record<string, Array<{ pattern?: RegExp }>> };
    const usciRules = vm.rules.usci;
    const patternRule = usciRules?.find((r: { pattern?: RegExp }) => r.pattern);
    expect(patternRule?.pattern).toEqual(/^[0-9A-Z]{18}$/);
  });

  it('has create button and correct form structure', async () => {
    vi.mocked(entQueryTaskApi.create).mockResolvedValue(mockCreated);
    mount(QueryTaskCreateDialog, {
      props: { modelValue: true },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();

    // Verify create button exists in rendered dialog
    const buttons = document.body.querySelectorAll('button');
    const createBtn = Array.from(buttons).find((b) => b.textContent?.includes('创建'));
    expect(createBtn).toBeTruthy();

    // Verify the form has expected structure
    const formItems = document.body.querySelectorAll('.el-form-item');
    expect(formItems.length).toBeGreaterThanOrEqual(4);
  });
});
