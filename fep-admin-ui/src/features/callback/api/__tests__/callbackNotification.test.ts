import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { callbackNotificationApi } from '../callbackNotification';

vi.mock('@/shared/http/client', () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
const get = vi.mocked(httpClient.get);
const put = vi.mocked(httpClient.put);

beforeEach(() => {
  get.mockReset();
  put.mockReset();
});

describe('callbackNotificationApi', () => {
  it('listUnread GETs /unread', async () => {
    get.mockResolvedValue([]);
    await callbackNotificationApi.listUnread();
    expect(get).toHaveBeenCalledWith('/api/v1/notifications/unread');
  });

  it('unreadCount GETs /unread/count', async () => {
    get.mockResolvedValue(0);
    await callbackNotificationApi.unreadCount();
    expect(get).toHaveBeenCalledWith('/api/v1/notifications/unread/count');
  });

  it('markRead PUTs /{id}/read', async () => {
    put.mockResolvedValue(undefined);
    await callbackNotificationApi.markRead('N1');
    expect(put).toHaveBeenCalledWith('/api/v1/notifications/N1/read');
  });
});
