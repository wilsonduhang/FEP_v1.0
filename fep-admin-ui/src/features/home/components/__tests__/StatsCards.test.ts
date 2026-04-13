import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import StatsCards from '../StatsCards.vue';

vi.mock('@/features/home/api/stats-api', () => ({
  statsApi: {
    getCards: vi.fn().mockResolvedValue({
      totalAmount: '123456789.50',
      successCount: 9999,
      todayMessageCount: 42,
      exceptionCount: 3,
    }),
  },
}));

async function flush() {
  await new Promise((r) => setTimeout(r, 0));
}

describe('StatsCards', () => {
  it('renders four cards with values from API', async () => {
    const w = mount(StatsCards, { global: { plugins: [ElementPlus] } });
    await flush();
    expect(w.text()).toContain('123456789.50');
    expect(w.text()).toContain('9999');
    expect(w.text()).toContain('42');
    expect(w.text()).toContain('3');
  });
});
