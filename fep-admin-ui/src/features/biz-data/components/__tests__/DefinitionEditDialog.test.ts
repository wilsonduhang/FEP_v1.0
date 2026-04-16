import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import ElementPlus from 'element-plus';
import DefinitionEditDialog from '../DefinitionEditDialog.vue';
import {
  bizMessageDefinitionApi,
  type DefinitionResponse,
} from '../../api/biz-message-definition-api';

vi.mock('../../api/biz-message-definition-api');

const mockDef: DefinitionResponse = {
  definitionId: 'D1',
  messageCode: '1001',
  messageName: '企业信息查询请求',
  businessTypeId: '3200',
  direction: 'OUTBOUND',
  fieldCount: 12,
  fieldSummary: 'summary',
  sampleXml: '<xml/>',
  definitionStatus: 'ENABLED',
  sortOrder: 1,
  createTime: '2026-04-15T10:00:00',
  updateTime: '2026-04-15T10:00:00',
};

describe('DefinitionEditDialog', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    vi.clearAllMocks();
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) {
      document.body.removeChild(document.body.firstChild);
    }
  });

  it('renders create dialog with form fields', async () => {
    mount(DefinitionEditDialog, {
      props: { modelValue: true, definition: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('新建报文定义');
    expect(body).toContain('报文编码');
    expect(body).toContain('报文名称');
    expect(body).toContain('方向');
  });

  it('disables messageCode in edit mode', async () => {
    mount(DefinitionEditDialog, {
      props: { modelValue: true, definition: mockDef },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const body = document.body.textContent || '';
    expect(body).toContain('编辑报文定义');
    // messageCode input should be disabled
    const inputs = document.body.querySelectorAll('input');
    const codeInput = Array.from(inputs).find(
      (inp) => (inp as HTMLInputElement).value === '1001',
    );
    expect(codeInput).toBeTruthy();
    expect((codeInput as HTMLInputElement).disabled).toBe(true);
  });

  it('has messageCode validation pattern for digits', async () => {
    const wrapper = mount(DefinitionEditDialog, {
      props: { modelValue: true, definition: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const vm = wrapper.vm as unknown as {
      rules: Record<string, Array<{ pattern?: RegExp }>>;
    };
    const codeRules = vm.rules.messageCode;
    const patternRule = codeRules?.find((r: { pattern?: RegExp }) => r.pattern);
    expect(patternRule?.pattern).toEqual(/^\d{1,5}$/);
  });

  it('defaults fieldCount and sortOrder to 0 in create mode', async () => {
    mount(DefinitionEditDialog, {
      props: { modelValue: true, definition: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    // el-input-number renders the value; look for "0"
    const spinButtons = document.body.querySelectorAll(
      '.el-input-number input',
    );
    const values = Array.from(spinButtons).map(
      (inp) => (inp as HTMLInputElement).value,
    );
    // Both fieldCount and sortOrder should be "0"
    expect(values.filter((v) => v === '0').length).toBeGreaterThanOrEqual(2);
  });

  it('has create button in create mode', async () => {
    mount(DefinitionEditDialog, {
      props: { modelValue: true, definition: null },
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    const buttons = document.body.querySelectorAll('button');
    const createBtn = Array.from(buttons).find((b) =>
      b.textContent?.includes('创建'),
    );
    expect(createBtn).toBeTruthy();
  });
});
