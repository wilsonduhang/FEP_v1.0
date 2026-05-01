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

const sampleRow: api.DirMapConfig = {
  messageType: '3001',
  messageName: '业务进展查询',
  accessRole: 'ACCEPTING_ORG',
  direction: 'INBOUND_PASSIVE',
  requiresFep: true,
  processingMode: 'MODE_1',
  updatedBy: 'system',
  updatedAt: '2026-04-29T00:00:00Z',
};

const sampleRowB: api.DirMapConfig = {
  messageType: '3107',
  messageName: '凭证查询',
  accessRole: 'INFO_SERVICE_ORG',
  direction: 'OUTBOUND_ACTIVE',
  requiresFep: false,
  processingMode: 'MODE_3',
  updatedBy: 'admin',
  updatedAt: '2026-04-29T00:00:00Z',
};

describe('DirMapConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.listDirMap).mockResolvedValue({
      records: [sampleRow],
      total: 1,
      pageNum: 1,
      pageSize: 100,
      totalPages: 1,
    });
  });

  it('应该渲染列表行', async () => {
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();
    expect(wrapper.text()).toContain('3001');
    expect(wrapper.text()).toContain('业务进展查询');
  });

  it('点击编辑后保存，调 update API + 显示成功（验 payload 完整）', async () => {
    vi.mocked(api.updateDirMap).mockResolvedValue(sampleRow);
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    // trigger click — feedback_unit_test_bypass 红线：禁用 vm.method() 直调
    const editButtons = wrapper.findAll('[data-test="btn-edit"]');
    expect(editButtons.length).toBeGreaterThan(0);
    await editButtons[0].trigger('click');
    await flushPromises();
    await wrapper.find('[data-test="btn-save"]').trigger('click');
    await flushPromises();

    // T6 quality reviewer P2 修复：toHaveBeenCalled 太宽，改 toHaveBeenCalledWith
    // 验证 path-vars + body shape（CLAUDE.md gate #2 业务断言）
    expect(api.updateDirMap).toHaveBeenCalledWith('3001', 'ACCEPTING_ORG', {
      direction: 'INBOUND_PASSIVE',
      requiresFep: true,
      processingMode: 'MODE_1',
      changeReason: '',
    });
    expect(ElMessage.success).toHaveBeenCalledWith('保存成功，已立即生效');
  });

  it('save 失败时不弹 success（错误 toast 由拦截器统一处理）', async () => {
    vi.mocked(api.updateDirMap).mockRejectedValue({ code: 'PARAM_4002', message: '行不存在' });
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    const editButtons = wrapper.findAll('[data-test="btn-edit"]');
    await editButtons[0].trigger('click');
    await flushPromises();
    await wrapper.find('[data-test="btn-save"]').trigger('click');
    await flushPromises();

    expect(api.updateDirMap).toHaveBeenCalled();
    expect(ElMessage.success).not.toHaveBeenCalled();
    // 页面不再调 ElMessage.error（拦截器已弹），故 mock 不被页面调用
    expect(ElMessage.error).not.toHaveBeenCalled();
  });

  it('history 拉取成功 → 渲染历史行', async () => {
    vi.mocked(api.listDirMapHistory).mockResolvedValue([
      {
        historyId: 'h1',
        oldDirection: 'INBOUND_PASSIVE',
        oldRequiresFep: true,
        oldMode: 'MODE_1',
        newDirection: 'OUTBOUND_ACTIVE',
        newRequiresFep: false,
        newMode: 'MODE_3',
        changedBy: 'admin1',
        changedAt: '2026-04-29T00:00:00Z',
        changeReason: '切换出向',
      },
    ]);
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    const historyButtons = wrapper.findAll('[data-test="btn-history"]');
    expect(historyButtons.length).toBeGreaterThan(0);
    await historyButtons[0].trigger('click');
    await flushPromises();

    expect(api.listDirMapHistory).toHaveBeenCalledWith('3001', 'ACCEPTING_ORG');
    // history drawer 内容已渲染
    expect(wrapper.text()).toContain('切换出向');
  });

  it('list 拉取失败时 rows 为空（不抛 unhandled rejection）', async () => {
    vi.mocked(api.listDirMap).mockRejectedValue({ code: 'INTERNAL_5001', message: 'DB down' });
    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    expect(api.listDirMap).toHaveBeenCalled();
    // 表格为空 — 不会渲染上一次的行，避免误导
    expect(wrapper.text()).not.toContain('业务进展查询');
  });

  it('编辑 row A 同时点 row B 历史不会污染 editRow（P1 数据正确性 fix）', async () => {
    // 列表 mock 改成 2 行 — 验证 editRow / historyRow 完全独立
    vi.mocked(api.listDirMap).mockResolvedValue({
      records: [sampleRow, sampleRowB],
      total: 2,
      pageNum: 1,
      pageSize: 100,
      totalPages: 1,
    });
    vi.mocked(api.updateDirMap).mockResolvedValue(sampleRow);
    vi.mocked(api.listDirMapHistory).mockResolvedValue([]);

    const wrapper = mount(DirMapConfigPage, globalOpts);
    await flushPromises();

    // 1. 在第 1 行（3001/ACCEPTING_ORG）打开编辑 drawer
    const editButtons = wrapper.findAll('[data-test="btn-edit"]');
    expect(editButtons.length).toBeGreaterThanOrEqual(2);
    await editButtons[0].trigger('click');
    await flushPromises();

    // 2. 不关闭 edit drawer 直接在第 2 行（3107/INFO_SERVICE_ORG）点历史
    const historyButtons = wrapper.findAll('[data-test="btn-history"]');
    await historyButtons[1].trigger('click');
    await flushPromises();

    // 3. 此时点击 Save 按钮 — 必须仍提交第 1 行的 path-vars
    await wrapper.find('[data-test="btn-save"]').trigger('click');
    await flushPromises();

    expect(api.updateDirMap).toHaveBeenCalledWith('3001', 'ACCEPTING_ORG', {
      direction: 'INBOUND_PASSIVE',
      requiresFep: true,
      processingMode: 'MODE_1',
      changeReason: '',
    });
    // 历史 API 调的是行 B（确认 historyRow 独立指向 B）
    expect(api.listDirMapHistory).toHaveBeenCalledWith('3107', 'INFO_SERVICE_ORG');
  });
});
