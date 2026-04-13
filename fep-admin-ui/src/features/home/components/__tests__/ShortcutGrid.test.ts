import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import ShortcutGrid from '../ShortcutGrid.vue';

vi.mock('@/features/home/api/shortcut-api', () => ({
  shortcutApi: {
    list: vi.fn().mockResolvedValue([
      {
        shortcutId: 's1',
        userId: 'u1',
        shortcutName: '融资申请',
        targetUrl: '/funding',
        icon: 'Money',
        sortOrder: 1,
        visible: true,
        createTime: '2026-04-12T10:00:00',
        updateTime: '2026-04-12T10:00:00',
      },
      {
        shortcutId: 's2',
        userId: 'u1',
        shortcutName: '建档管理',
        targetUrl: '/archive',
        icon: 'Folder',
        sortOrder: 2,
        visible: true,
        createTime: '2026-04-12T10:00:00',
        updateTime: '2026-04-12T10:00:00',
      },
      {
        shortcutId: 's3',
        userId: 'u1',
        shortcutName: '隐藏项',
        targetUrl: '/hidden',
        icon: 'View',
        sortOrder: 3,
        visible: false,
        createTime: '2026-04-12T10:00:00',
        updateTime: '2026-04-12T10:00:00',
      },
    ]),
    reorder: vi.fn().mockResolvedValue(undefined),
    toggleVisibility: vi.fn().mockResolvedValue(undefined),
  },
}));

import { shortcutApi } from '@/features/home/api/shortcut-api';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('ShortcutGrid', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('only renders visible shortcuts', async () => {
    const w = mount(ShortcutGrid, { global: { plugins: [ElementPlus] } });
    await flush();
    expect(w.text()).toContain('融资申请');
    expect(w.text()).toContain('建档管理');
    expect(w.text()).not.toContain('隐藏项');
  });

  it('reorder button sends reorder request', async () => {
    const w = mount(ShortcutGrid, { global: { plugins: [ElementPlus] } });
    await flush();
    const upButtons = w.findAll('button.btn-move-up');
    expect(upButtons.length).toBeGreaterThan(1);
    await upButtons[1].trigger('click');
    await flush();
    expect(shortcutApi.reorder).toHaveBeenCalled();
  });
});
