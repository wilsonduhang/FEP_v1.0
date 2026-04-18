import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subBusinessSceneApi } from '../sub-business-scene-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('subBusinessSceneApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('search() GETs with keyword + businessTypeId + pagination', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    await subBusinessSceneApi.search({
      pageNum: 1,
      pageSize: 10,
      keyword: 'x',
      businessTypeId: '3000',
    });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/scenes', {
      params: { pageNum: 1, pageSize: 10, keyword: 'x', businessTypeId: '3000' },
    });
  });

  it('getById() GETs /{id}', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ sceneId: '1' });
    await subBusinessSceneApi.getById('1');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/scenes/1');
  });

  it('create() POSTs AUTO mode without importTemplatePath', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ sceneId: '1' });
    await subBusinessSceneApi.create({
      sceneName: 'scene_a',
      businessTypeId: '3000',
      pushMethod: 'AUTO',
      requestUrl: 'https://x.example.com',
      sortOrder: 1,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/submission/scenes',
      expect.objectContaining({ pushMethod: 'AUTO', sceneName: 'scene_a' }),
    );
  });

  it('create() POSTs MANUAL mode with importTemplatePath', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ sceneId: '1' });
    await subBusinessSceneApi.create({
      sceneName: 'scene_b',
      businessTypeId: '3000',
      pushMethod: 'MANUAL',
      importTemplatePath: '/tmp/t.xml',
      requestUrl: 'https://x.example.com',
      sortOrder: 2,
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/submission/scenes',
      expect.objectContaining({ pushMethod: 'MANUAL', importTemplatePath: '/tmp/t.xml' }),
    );
  });

  it('update() PUTs to /{id} with body', async () => {
    vi.mocked(httpClient.put).mockResolvedValue(undefined);
    await subBusinessSceneApi.update('1', {
      sceneName: 's',
      businessTypeId: '3000',
      pushMethod: 'AUTO',
      requestUrl: 'https://x.example.com',
      sortOrder: 1,
    });
    expect(httpClient.put).toHaveBeenCalledWith(
      '/api/v1/submission/scenes/1',
      expect.objectContaining({ sceneName: 's' }),
    );
  });

  it('toggleStatus() PATCH /{id}/status with no body (single arg)', async () => {
    vi.mocked(httpClient.patch).mockResolvedValue({
      sceneId: '1',
      sceneStatus: 'DISABLED',
    });
    await subBusinessSceneApi.toggleStatus('1');
    expect(httpClient.patch).toHaveBeenCalledWith('/api/v1/submission/scenes/1/status');
    expect(vi.mocked(httpClient.patch).mock.calls[0].length).toBe(1);
  });

  it('remove() DELETE /{id}', async () => {
    vi.mocked(httpClient.delete).mockResolvedValue(undefined);
    await subBusinessSceneApi.remove('1');
    expect(httpClient.delete).toHaveBeenCalledWith('/api/v1/submission/scenes/1');
  });
});
