import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subMessageSummaryApi } from '../sub-message-summary-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
  },
}));

describe('subMessageSummaryApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getSummary() GETs /message-summary and returns list', async () => {
    vi.mocked(httpClient.get).mockResolvedValue([
      {
        messageType: '3001',
        messageName: '查询请求',
        businessTypeId: 'BIZ001',
        totalCount: 100,
        pushedCount: 80,
        pendingCount: 20,
      },
    ]);
    const result = await subMessageSummaryApi.getSummary();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/message-summary');
    expect(result).toHaveLength(1);
    expect(result[0].messageType).toBe('3001');
    expect(result[0].pushedCount).toBe(80);
    expect(result[0].pendingCount).toBe(20);
  });
});
