import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { callbackCredentialApi } from '../callbackCredential';

vi.mock('@/shared/http/client', () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));
const get = vi.mocked(httpClient.get);
const post = vi.mocked(httpClient.post);
const put = vi.mocked(httpClient.put);
const del = vi.mocked(httpClient.delete);

beforeEach(() => {
  get.mockReset();
  post.mockReset();
  put.mockReset();
  del.mockReset();
});

describe('callbackCredentialApi', () => {
  it('list GETs the base url', async () => {
    get.mockResolvedValue([]);
    await callbackCredentialApi.list();
    expect(get).toHaveBeenCalledWith('/api/v1/callback/credentials');
  });

  it('get keys by interfaceId', async () => {
    get.mockResolvedValue({});
    await callbackCredentialApi.get('IF-001');
    expect(get).toHaveBeenCalledWith('/api/v1/callback/credentials/IF-001');
  });

  it('create POSTs the request body', async () => {
    post.mockResolvedValue({});
    const req = { interfaceId: 'IF-001', authType: 'TOKEN' as const, token: 'plain' };
    await callbackCredentialApi.create(req);
    expect(post).toHaveBeenCalledWith('/api/v1/callback/credentials', req);
  });

  it('update PUTs to interfaceId path', async () => {
    put.mockResolvedValue({});
    await callbackCredentialApi.update('IF-001', { token: 'new' });
    expect(put).toHaveBeenCalledWith('/api/v1/callback/credentials/IF-001', { token: 'new' });
  });

  it('delete DELETEs interfaceId path', async () => {
    del.mockResolvedValue(undefined);
    await callbackCredentialApi.delete('IF-001');
    expect(del).toHaveBeenCalledWith('/api/v1/callback/credentials/IF-001');
  });
});
