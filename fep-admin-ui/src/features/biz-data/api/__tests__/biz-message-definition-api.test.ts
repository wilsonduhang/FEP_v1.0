import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { bizMessageDefinitionApi } from '../biz-message-definition-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('bizMessageDefinitionApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('search calls GET /api/v1/bizdata/definitions with params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      totalPages: 0,
    });
    await bizMessageDefinitionApi.search({ pageNum: 1, pageSize: 20, direction: 'OUTBOUND' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/bizdata/definitions', {
      params: { pageNum: 1, pageSize: 20, direction: 'OUTBOUND' },
    });
  });

  it('getById calls GET with id in path', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({});
    await bizMessageDefinitionApi.getById('D-001');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/bizdata/definitions/D-001');
  });

  it('create calls POST with request body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await bizMessageDefinitionApi.create({
      messageCode: '1001',
      messageName: '企业基本信息查询',
      direction: 'OUTBOUND',
      fieldCount: 12,
      sortOrder: 1,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/bizdata/definitions',
      expect.objectContaining({ messageCode: '1001' }),
    );
  });

  it('update calls PUT with id and body', async () => {
    vi.mocked(httpClient.put).mockResolvedValue({});
    await bizMessageDefinitionApi.update('D-001', { messageName: '更新名称' });
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/bizdata/definitions/D-001',
      expect.objectContaining({ messageName: '更新名称' }),
    );
  });

  it('toggleStatus calls PUT on /definitions/{id}/toggle-status', async () => {
    vi.mocked(httpClient.put).mockResolvedValue({});
    await bizMessageDefinitionApi.toggleStatus('D-001');
    expect(httpClient.put).toHaveBeenCalledWith('/api/v1/bizdata/definitions/D-001/toggle-status');
  });

  it('delete calls DELETE', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await bizMessageDefinitionApi.delete('D-001');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/bizdata/definitions/D-001');
  });
});
