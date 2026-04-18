import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subOutputInterfaceApi } from '../sub-output-interface-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('subOutputInterfaceApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('search() GETs with keyword + pagination params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    await subOutputInterfaceApi.search({ pageNum: 1, pageSize: 10, keyword: 'x' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/output-interfaces', {
      params: { pageNum: 1, pageSize: 10, keyword: 'x' },
    });
  });

  it('getById() GETs /{id}', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ interfaceId: '42' });
    await subOutputInterfaceApi.getById('42');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/output-interfaces/42');
  });

  it('create() POSTs to base with body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ interfaceId: '99' });
    await subOutputInterfaceApi.create({
      interfaceName: 'n',
      interfaceUrl: 'https://x.example.com',
      authType: 'NONE',
      timeoutSeconds: 30,
      retryCount: 3,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/submission/output-interfaces',
      expect.objectContaining({ interfaceName: 'n', authType: 'NONE' }),
    );
  });

  it('update() PUTs to /{id} with body', async () => {
    vi.mocked(httpClient.put).mockResolvedValue({ interfaceId: '42' });
    await subOutputInterfaceApi.update('42', {
      interfaceName: 'n',
      interfaceUrl: 'https://x.example.com',
      authType: 'TOKEN',
      timeoutSeconds: 30,
      retryCount: 3,
    });
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/submission/output-interfaces/42',
      expect.objectContaining({ authType: 'TOKEN' }),
    );
  });

  it('toggleStatus() PATCH /{id}/status with no body (single arg)', async () => {
    vi.mocked(httpClient.patch).mockResolvedValue({
      interfaceId: '42',
      interfaceStatus: 'DISABLED',
    });
    await subOutputInterfaceApi.toggleStatus('42');
    expect(httpClient.patch).toHaveBeenCalledWith('/api/v1/submission/output-interfaces/42/status');
    expect(vi.mocked(httpClient.patch).mock.calls[0].length).toBe(1);
  });

  it('test() POST /{id}/test returns Boolean directly', async () => {
    vi.mocked(httpClient.post).mockResolvedValue(true);
    const result = await subOutputInterfaceApi.test('42');
    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/submission/output-interfaces/42/test');
    expect(result).toBe(true);
  });

  it('remove() DELETE /{id}', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await subOutputInterfaceApi.remove('42');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/submission/output-interfaces/42');
  });
});
