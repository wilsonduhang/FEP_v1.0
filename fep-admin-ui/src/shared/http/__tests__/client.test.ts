import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest';
import { httpClient } from '../client';
import { TokenStorage } from '../token-storage';

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn() },
}));

describe('httpClient', () => {
  beforeEach(() => {
    localStorage.clear();
    httpClient.defaults.adapter = vi.fn();
  });
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('injects Authorization header when token present', async () => {
    TokenStorage.set('tkn-1');
    const adapter = vi.fn().mockResolvedValue({
      data: { code: '200', message: 'ok', data: { id: 1 } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: { headers: {} as any },
    });
    httpClient.defaults.adapter = adapter;

    await httpClient.get('/api/v1/ping');

    expect(adapter).toHaveBeenCalledOnce();
    const cfg = adapter.mock.calls[0][0];
    expect(cfg.headers.Authorization).toBe('Bearer tkn-1');
  });

  it('unwraps successful ApiResult to data field', async () => {
    httpClient.defaults.adapter = vi.fn().mockResolvedValue({
      data: { code: '200', message: 'ok', data: { id: 42 } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: { headers: {} as any },
    });

    const result = await httpClient.get<{ id: number }>('/api/v1/ping');
    expect(result).toEqual({ id: 42 });
  });

  it('rejects and fires auth:expired on HTTP 401', async () => {
    TokenStorage.set('stale');
    httpClient.defaults.adapter = vi.fn().mockRejectedValue({
      response: {
        status: 401,
        data: { code: 'ERR_AUTH_UNAUTHORIZED', message: 'expired', data: null },
      },
    });
    const listener = vi.fn();
    window.addEventListener('auth:expired', listener);

    await expect(httpClient.get('/api/v1/any')).rejects.toBeDefined();
    expect(TokenStorage.get()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();

    window.removeEventListener('auth:expired', listener);
  });

  it('rejects with ApiResult body when code is non-SUCCESS but HTTP 200', async () => {
    const body = { code: 'ERR_BIZ_001', message: 'bad input', data: null };
    httpClient.defaults.adapter = vi.fn().mockResolvedValue({
      data: body,
      status: 200,
      statusText: 'OK',
      headers: {},
      config: { headers: {} as any },
    });

    await expect(httpClient.post('/api/v1/x', {})).rejects.toEqual(body);
  });
});
