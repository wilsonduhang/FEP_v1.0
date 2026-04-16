import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { entQueryTaskApi } from '../ent-query-task-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('entQueryTaskApi', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('search calls GET /api/v1/ent-query/tasks with params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      totalPages: 0,
    });
    await entQueryTaskApi.search({ pageNum: 1, pageSize: 20, queryType: 'REALTIME' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/ent-query/tasks', {
      params: { pageNum: 1, pageSize: 20, queryType: 'REALTIME' },
    });
  });

  it('getById calls GET with taskId in path', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({});
    await entQueryTaskApi.getById('T-001');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/ent-query/tasks/T-001');
  });

  it('create calls POST with request body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await entQueryTaskApi.create({
      enterpriseId: 'E1',
      queryType: 'REALTIME',
      usci: '91310000MA1K40XK7A',
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/ent-query/tasks',
      expect.objectContaining({ usci: '91310000MA1K40XK7A' }),
    );
  });

  it('execute calls POST on /tasks/{id}/execute', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await entQueryTaskApi.execute('T-001');
    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/ent-query/tasks/T-001/execute');
  });

  it('listResults calls GET on /tasks/{id}/results', async () => {
    vi.mocked(httpClient.get).mockResolvedValue([]);
    await entQueryTaskApi.listResults('T-001');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/ent-query/tasks/T-001/results');
  });

  it('delete calls DELETE', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await entQueryTaskApi.delete('T-001');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/ent-query/tasks/T-001');
  });
});
