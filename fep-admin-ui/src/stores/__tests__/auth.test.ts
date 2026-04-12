import { beforeEach, describe, it, expect, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '../auth';
import { TokenStorage } from '@/shared/http/token-storage';

vi.mock('@/features/auth/api/auth-api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
    captcha: vi.fn(),
    refresh: vi.fn(),
  },
}));

import { authApi } from '@/features/auth/api/auth-api';

const sampleLoginResponse = {
  accessToken: 'AT',
  refreshToken: 'RT',
  userId: 'u1',
  userAccount: 'alice',
  userName: '张三',
  roleCodes: ['ADMIN'],
  passwordChangeRequired: false,
};

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('isAuthenticated is false initially', () => {
    const store = useAuthStore();
    expect(store.isAuthenticated).toBe(false);
  });

  it('login stores tokens and flat profile on success', async () => {
    (authApi.login as any).mockResolvedValue(sampleLoginResponse);
    const store = useAuthStore();
    await store.login({ account: 'alice', password: 'Abc12345', captchaId: 'cap-1', captchaCode: '1234' });

    expect(TokenStorage.get()).toBe('AT');
    expect(TokenStorage.getRefresh()).toBe('RT');
    expect(store.isAuthenticated).toBe(true);
    expect(store.profile?.userAccount).toBe('alice');
    expect(store.profile?.userName).toBe('张三');
    expect(store.profile?.roleCodes).toEqual(['ADMIN']);
    expect(store.profile?.passwordChangeRequired).toBe(false);
  });

  it('logout clears tokens and resets profile', async () => {
    TokenStorage.set('AT');
    TokenStorage.setRefresh('RT');
    const store = useAuthStore();
    store.$patch({ profile: { ...sampleLoginResponse } });

    await store.logout();

    expect(TokenStorage.get()).toBeNull();
    expect(store.profile).toBeNull();
    expect(authApi.logout).toHaveBeenCalledOnce();
  });

  it('logout tolerates backend failure and still clears local state', async () => {
    (authApi.logout as any).mockRejectedValue(new Error('network'));
    TokenStorage.set('AT');
    const store = useAuthStore();
    store.$patch({ profile: { ...sampleLoginResponse } });

    await store.logout();

    expect(TokenStorage.get()).toBeNull();
    expect(store.profile).toBeNull();
  });

  it('auth:expired event triggers local logout without API call', () => {
    TokenStorage.set('AT');
    const store = useAuthStore();
    store.bindExpiryListener();

    window.dispatchEvent(new CustomEvent('auth:expired'));

    expect(TokenStorage.get()).toBeNull();
    expect(store.profile).toBeNull();
    expect(authApi.logout).not.toHaveBeenCalled();
  });
});
