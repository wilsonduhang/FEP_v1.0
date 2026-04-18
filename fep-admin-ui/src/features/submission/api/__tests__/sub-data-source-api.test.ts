import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subDataSourceApi } from '../sub-data-source-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('subDataSourceApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('search() GETs with keyword + pagination only (no status filter)', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    await subDataSourceApi.search({ pageNum: 1, pageSize: 10, keyword: 'x' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/data-sources', {
      params: { pageNum: 1, pageSize: 10, keyword: 'x' },
    });
  });

  it('getById() GETs /{id}', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ sourceId: '1' });
    await subDataSourceApi.getById('1');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/data-sources/1');
  });

  it('create() POSTs to base with body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ sourceId: '1' });
    await subDataSourceApi.create({
      sourceName: 'n',
      contactAddress: 'a',
      contactPhone: '13800138000',
      pushEnabled: false,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/submission/data-sources',
      expect.objectContaining({ sourceName: 'n', pushEnabled: false }),
    );
  });

  it('update() PUTs to /{id} with body', async () => {
    vi.mocked(httpClient.put).mockResolvedValue(undefined);
    await subDataSourceApi.update('1', {
      sourceName: 'n',
      contactAddress: 'a',
      contactPhone: '13800138000',
      pushEnabled: true,
    });
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/submission/data-sources/1',
      expect.objectContaining({ pushEnabled: true }),
    );
  });

  it('remove() DELETE /{id}', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await subDataSourceApi.remove('1');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/submission/data-sources/1');
  });
});
