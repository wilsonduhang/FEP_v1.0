import { mount, flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import MessageSummaryPage from '../MessageSummaryPage.vue';
import { subMessageSummaryApi } from '../../api/sub-message-summary-api';

vi.mock('../../api/sub-message-summary-api', () => ({
  subMessageSummaryApi: {
    getSummary: vi.fn(),
  },
}));

const mockPush = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
}));

const globalOpts = { global: { plugins: [ElementPlus] } };

describe('MessageSummaryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(subMessageSummaryApi.getSummary).mockResolvedValue([
      {
        messageType: '3001',
        messageName: '查询请求',
        businessTypeId: 'BIZ001',
        totalCount: 100,
        pushedCount: 80,
        pendingCount: 20,
      },
    ]);
  });

  it('loads summary on mount', async () => {
    mount(MessageSummaryPage, globalOpts);
    await flushPromises();
    expect(subMessageSummaryApi.getSummary).toHaveBeenCalledOnce();
  });

  it('pushes to /report/records with messageType query on navigate event', async () => {
    const wrapper = mount(MessageSummaryPage, globalOpts);
    await flushPromises();
    (wrapper.vm as unknown as { onNavigate: (p: { messageType: string }) => void }).onNavigate({
      messageType: '3001',
    });
    expect(mockPush).toHaveBeenCalledWith({
      path: '/report/records',
      query: { messageType: '3001' },
    });
  });
});
