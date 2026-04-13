import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import TodoList from '../TodoList.vue';

vi.mock('@/features/home/api/todo-api', () => ({
  todoApi: {
    search: vi.fn().mockResolvedValue({
      records: [
        {
          todoId: 't1',
          taskType: 'FUNDING_APPLY',
          title: '融资申请审批',
          priority: 'URGENT',
          todoStatus: 'PENDING',
          targetUrl: '/funding/t1',
          assignedUserId: 'u1',
          deadline: null,
          completedTime: null,
          createTime: '2026-04-12T10:00:00',
          updateTime: '2026-04-12T10:00:00',
        },
      ],
      total: 1,
      pageNum: 1,
      pageSize: 10,
      totalPages: 1,
    }),
    startProcessing: vi.fn(),
    complete: vi.fn(),
    delete: vi.fn(),
  },
}));

import { todoApi } from '@/features/home/api/todo-api';

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('TodoList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads and renders first page of todos', async () => {
    const w = mount(TodoList, { global: { plugins: [ElementPlus] } });
    await flush();
    expect(todoApi.search).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 });
    expect(w.text()).toContain('融资申请审批');
    expect(w.text()).toContain('URGENT');
  });

  it('maps priority URGENT to danger tag type', async () => {
    const w = mount(TodoList, { global: { plugins: [ElementPlus] } });
    await flush();
    const tag = w.find('.el-tag--danger');
    expect(tag.exists()).toBe(true);
  });

  it('startProcessing click invokes api and emits update', async () => {
    (todoApi.startProcessing as unknown as { mockResolvedValue: (v: unknown) => void }).mockResolvedValue({ todoId: 't1' });
    const w = mount(TodoList, { global: { plugins: [ElementPlus] } });
    await flush();
    const btn = w.find('button.btn-start');
    await btn.trigger('click');
    await flush();
    expect(todoApi.startProcessing).toHaveBeenCalledWith('t1');
    expect(w.emitted('update')).toBeTruthy();
  });
});
