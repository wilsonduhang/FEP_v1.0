import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import TodoCountBadge from '../TodoCountBadge.vue';

vi.mock('@/features/home/api/todo-api', () => ({
  todoApi: {
    countPending: vi.fn().mockResolvedValue(7),
  },
}));

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('TodoCountBadge', () => {
  it('fetches and displays pending count on mount', async () => {
    const w = mount(TodoCountBadge, { global: { plugins: [ElementPlus] } });
    await flush();
    expect(w.text()).toContain('7');
  });
});
