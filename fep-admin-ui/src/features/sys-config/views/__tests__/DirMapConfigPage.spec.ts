import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import ElementPlus, { ElMessage } from 'element-plus';
import DirMapConfigPage from '../DirMapConfigPage.vue';
import * as api from '../../api/dir-map-api';

vi.mock('../../api/dir-map-api');

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

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('DirMapConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.listDirMap).mockResolvedValue({
      records: [
        {
          messageType: '3001',
          messageName: '业务进展查询',
          accessRole: 'ACCEPTING_ORG',
          direction: 'INBOUND_PASSIVE',
          requiresFep: true,
          processingMode: 'MODE_1',
          updatedBy: 'system',
          updatedAt: '2026-04-29T00:00:00Z',
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 100,
    });
  });

  it('应该渲染列表行', async () => {
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();
    expect(wrapper.text()).toContain('3001');
    expect(wrapper.text()).toContain('业务进展查询');
  });

  it('点击编辑后保存，调 update API + 显示成功', async () => {
    vi.mocked(api.updateDirMap).mockResolvedValue({} as never);
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    // trigger click on edit button — feedback_unit_test_bypass 红线：禁用 vm.method() 直调
    // findAll()[0] 替代 find()，多行 mock 时仍精确点击第一行
    const editButtons = wrapper.findAll('[data-test="btn-edit"]');
    expect(editButtons.length).toBeGreaterThan(0);
    await editButtons[0].trigger('click');
    await flushPromises();
    await wrapper.find('[data-test="btn-save"]').trigger('click');
    await flushPromises();

    expect(api.updateDirMap).toHaveBeenCalled();
    expect(ElMessage.success).toHaveBeenCalledWith('保存成功，已立即生效');
  });
});
