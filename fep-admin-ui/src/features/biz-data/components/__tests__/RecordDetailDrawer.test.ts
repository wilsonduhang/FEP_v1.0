import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import RecordDetailDrawer from '../RecordDetailDrawer.vue';
import { bizMessageRecordApi, type RecordResponse } from '../../api/biz-message-record-api';

vi.mock('../../api/biz-message-record-api');

const mockRecord: RecordResponse = {
  recordId: 'R1',
  messageCode: '1001',
  serialNo: 'SN001',
  senderNode: 'NODE_A',
  receiverNode: 'NODE_B',
  direction: 'OUTBOUND',
  processStatus: 'SUCCESS',
  businessNo: 'BIZ001',
  amount: '99.99',
  xmlContent: '<root><msg>hello</msg></root>',
  entryMethod: 'API',
  accessCount: 5,
  errorMessage: null,
  processTime: '2026-04-15T10:00:05',
  createTime: '2026-04-15T10:00:00',
  updateTime: '2026-04-15T10:00:05',
};

const failedRecord: RecordResponse = {
  ...mockRecord,
  recordId: 'R2',
  processStatus: 'FAILED',
  errorMessage: '报文校验失败：缺少必填字段',
};

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('RecordDetailDrawer', () => {
  beforeEach(() => vi.clearAllMocks());

  it('loads record on open', async () => {
    vi.mocked(bizMessageRecordApi.getById).mockResolvedValue(mockRecord);
    const wrapper = mount(RecordDetailDrawer, {
      props: { modelValue: true, recordId: 'R1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    expect(bizMessageRecordApi.getById).toHaveBeenCalledWith('R1');
    expect(wrapper.text()).toContain('R1');
    expect(wrapper.text()).toContain('SN001');
    wrapper.unmount();
  });

  it('shows XML preview with monospace styling', async () => {
    vi.mocked(bizMessageRecordApi.getById).mockResolvedValue(mockRecord);
    const wrapper = mount(RecordDetailDrawer, {
      props: { modelValue: true, recordId: 'R1' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    const xmlPre = wrapper.find('.xml-preview');
    expect(xmlPre.exists()).toBe(true);
    expect(xmlPre.text()).toContain('<root><msg>hello</msg></root>');
    wrapper.unmount();
  });

  it('shows error message in red for FAILED records', async () => {
    vi.mocked(bizMessageRecordApi.getById).mockResolvedValue(failedRecord);
    const wrapper = mount(RecordDetailDrawer, {
      props: { modelValue: true, recordId: 'R2' },
      ...globalOpts,
      attachTo: document.body,
    });
    await flushPromises();
    const errorEl = wrapper.find('.error-message');
    expect(errorEl.exists()).toBe(true);
    expect(errorEl.text()).toContain('报文校验失败');
    wrapper.unmount();
  });
});
