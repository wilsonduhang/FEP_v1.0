import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { callbackDlqApi } from '../callbackDlq';

vi.mock('@/shared/http/client', () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
const get = vi.mocked(httpClient.get);
const post = vi.mocked(httpClient.post);

beforeEach(() => {
  get.mockReset();
  post.mockReset();
});

describe('callbackDlqApi', () => {
  it('list passes 0-based page + size as query params', async () => {
    get.mockResolvedValue([]);
    await callbackDlqApi.list({ page: 0, size: 20 });
    expect(get).toHaveBeenCalledWith('/api/v1/callback/dlq', { params: { page: 0, size: 20 } });
  });

  it('list page distinct (page=2 propagates verbatim, no off-by-one)', async () => {
    get.mockResolvedValue([]);
    await callbackDlqApi.list({ page: 2, size: 20 });
    expect(get).toHaveBeenCalledWith('/api/v1/callback/dlq', { params: { page: 2, size: 20 } });
  });

  it('replay POSTs to replay path', async () => {
    post.mockResolvedValue({ newQueueId: 'N1', originalDlqId: 'D1', replayedAt: 't' });
    await callbackDlqApi.replay('D1');
    expect(post).toHaveBeenCalledWith('/api/v1/callback/dlq/D1/replay');
  });

  it('chain GETs chain path', async () => {
    get.mockResolvedValue([]);
    await callbackDlqApi.chain('D1');
    expect(get).toHaveBeenCalledWith('/api/v1/callback/dlq/D1/chain');
  });
});
