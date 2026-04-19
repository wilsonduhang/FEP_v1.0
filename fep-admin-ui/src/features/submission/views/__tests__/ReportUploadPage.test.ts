import { describe, expect, it, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import ReportUploadPage from '../ReportUploadPage.vue';
import { subReportApi } from '../../api/sub-report-api';

vi.mock('../../api/sub-report-api', () => ({
  subReportApi: { uploadRecord: vi.fn() },
}));

vi.mock('@/features/biz-data/api/biz-message-definition-api', () => ({
  bizMessageDefinitionApi: {
    search: vi.fn().mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 100,
      totalPages: 0,
    }),
  },
}));

import { bizMessageDefinitionApi } from '@/features/biz-data/api/biz-message-definition-api';

const mockUpload = vi.mocked(subReportApi.uploadRecord);

beforeEach(() => {
  mockUpload.mockReset();
  vi.mocked(bizMessageDefinitionApi.search).mockClear();
});

describe('ReportUploadPage', () => {
  it('renders MockBadge with "P1 就绪" notice', () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    expect(wrapper.text()).toContain('P1 就绪');
  });

  it('submit button is disabled when messageType/dataCount not filled', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const submitBtn = wrapper.find('[data-test="submit-upload"]');
    expect(submitBtn.attributes('disabled')).toBeDefined();
  });

  it('rejects file with unsupported extension via before-upload hook', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    const bad = new File(['x'], 'data.txt');
    expect(vm.beforeFileUpload(bad)).toBe(false);
  });

  it('accepts .xlsx and auto-fills messageName from filename', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    const ok = new File(['x'], '电子合同信息流转.xlsx');
    expect(vm.beforeFileUpload(ok)).toBe(false);
    expect(vm.form.messageName).toBe('电子合同信息流转');
  });

  it('calls subReportApi.uploadRecord on submit with 5 params', async () => {
    mockUpload.mockResolvedValue({ recordId: 'R1' } as any);
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    vm.form.messageType = '3001';
    vm.form.messageName = '查询请求';
    vm.form.fileName = 'a.xlsx';
    vm.form.dataCount = 5;
    vm.form.entryBy = 'zhangsan';
    await vm.onSubmit();
    expect(mockUpload).toHaveBeenCalledWith(
      expect.objectContaining({
        messageType: '3001',
        messageName: '查询请求',
        dataCount: 5,
        entryBy: 'zhangsan',
      }),
    );
  });

  it('keeps submit disabled when fileName is missing', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    vm.form.messageType = '3001';
    vm.form.messageName = 'X';
    vm.form.fileName = '';
    vm.form.dataCount = 5;
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test="submit-upload"]').attributes('disabled')).toBeDefined();
  });

  it('keeps submit disabled when messageName is empty even if file chosen', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    vm.form.messageType = '3001';
    vm.form.messageName = '';
    vm.form.fileName = 'a.xlsx';
    vm.form.dataCount = 5;
    await wrapper.vm.$nextTick();
    expect(wrapper.find('[data-test="submit-upload"]').attributes('disabled')).toBeDefined();
  });

  it('does NOT mutate form.fileName when extension is illegal', async () => {
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    const vm = wrapper.vm as any;
    vm.form.messageName = '';
    const bad = new File(['x'], 'notes.txt');
    vm.beforeFileUpload(bad);
    expect(vm.form.fileName).toBe('');
    expect(vm.form.messageName).toBe('');
  });

  it('loadMessageTypeOptions fetches via bizMessageDefinitionApi.search with messageCode field', async () => {
    vi.mocked(bizMessageDefinitionApi.search).mockResolvedValueOnce({
      records: [{ messageCode: '3001', messageName: '查询请求', definitionId: 'D1' } as any],
      total: 1,
      pageNum: 1,
      pageSize: 100,
      totalPages: 1,
    });
    const wrapper = mount(ReportUploadPage, { global: { plugins: [ElementPlus] } });
    await flushPromises();
    expect(bizMessageDefinitionApi.search).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 });
    const vm = wrapper.vm as any;
    expect(vm.messageTypeOptions?.value?.[0]?.code ?? vm.messageTypeOptions?.[0]?.code).toBe('3001');
  });
});
