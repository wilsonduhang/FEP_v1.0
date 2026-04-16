import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { entAuthLetterApi } from '../ent-auth-letter-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('entAuthLetterApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('search calls GET /api/v1/ent-query/auth-letters with params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      totalPages: 0,
    });
    await entAuthLetterApi.search({ pageNum: 1, pageSize: 20, letterStatus: 'DRAFT' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/ent-query/auth-letters', {
      params: { pageNum: 1, pageSize: 20, letterStatus: 'DRAFT' },
    });
  });

  it('getById calls GET with letterId in path', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({});
    await entAuthLetterApi.getById('L-001');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/ent-query/auth-letters/L-001');
  });

  it('create calls POST with request body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await entAuthLetterApi.create({
      enterpriseId: 'E1',
      authType: 'PAPER',
      authorizedUsci: '91310000MA1K40XK7A',
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/ent-query/auth-letters',
      expect.objectContaining({ authorizedUsci: '91310000MA1K40XK7A' }),
    );
  });

  it('update calls PUT with letterId and body', async () => {
    vi.mocked(httpClient.put).mockResolvedValue({});
    await entAuthLetterApi.update('L-001', {
      enterpriseId: 'E1',
      authType: 'ELECTRONIC',
      authorizedUsci: '91310000MA1K40XK7A',
    });
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/ent-query/auth-letters/L-001',
      expect.objectContaining({ authType: 'ELECTRONIC' }),
    );
  });

  it('submit calls POST on /auth-letters/{id}/submit', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await entAuthLetterApi.submit('L-001');
    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/ent-query/auth-letters/L-001/submit');
  });

  it('delete calls DELETE', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await entAuthLetterApi.delete('L-001');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/ent-query/auth-letters/L-001');
  });
});
