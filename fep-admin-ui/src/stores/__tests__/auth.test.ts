import { beforeEach, describe, it, expect, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '../auth';
import { TokenStorage } from '@/shared/http/token-storage';

vi.mock('@/features/auth/api/auth-api', () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn().mockResolvedValue(undefined),
    captcha: vi.fn(),
    getPublicKey: vi.fn(),
    refresh: vi.fn(),
    getMe: vi.fn(),
  },
}));

import { authApi } from '@/features/auth/api/auth-api';

const sampleLoginResponse = {
  accessToken: 'AT',
  refreshToken: 'RT',
  userId: 'u1',
  userAccount: 'alicex',
  userName: '张三',
  roleCodes: ['ADMIN'],
  passwordChangeRequired: false,
};

const sampleUserInfo = {
  userId: 'u1',
  userAccount: 'alicex',
  userName: '张三',
  phone: null,
  email: 'alice@example.com',
  department: '研发部',
  roleCodes: ['ADMIN'],
  permissions: ['sys:user:list', 'sys:user:create'],
  menuTree: [],
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

  it('login stores tokens and populates profile from getMe', async () => {
    (authApi.login as any).mockResolvedValue(sampleLoginResponse);
    (authApi.getMe as any).mockResolvedValue(sampleUserInfo);
    const store = useAuthStore();
    await store.login({
      account: 'alicex',
      password: 'Abc12345',
      captchaId: 'cap-1',
      captchaCode: '1234',
    });

    expect(TokenStorage.get()).toBe('AT');
    expect(TokenStorage.getRefresh()).toBe('RT');
    expect(store.isAuthenticated).toBe(true);
    expect(store.profile?.userAccount).toBe('alicex');
    expect(store.profile?.userName).toBe('张三');
    expect(store.profile?.roleCodes).toEqual(['ADMIN']);
    expect(store.profile?.permissions).toEqual([
      'sys:user:list',
      'sys:user:create',
    ]);
  });

  it('logout clears tokens and resets profile', async () => {
    TokenStorage.set('AT');
    TokenStorage.setRefresh('RT');
    const store = useAuthStore();
    store.$patch({ profile: sampleUserInfo });

    await store.logout();

    expect(TokenStorage.get()).toBeNull();
    expect(store.profile).toBeNull();
    expect(authApi.logout).toHaveBeenCalledOnce();
  });

  it('logout tolerates backend failure and still clears local state', async () => {
    (authApi.logout as any).mockRejectedValue(new Error('network'));
    TokenStorage.set('AT');
    const store = useAuthStore();
    store.$patch({ profile: sampleUserInfo });

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

  it('login auto-fetches user info after token stored', async () => {
    (authApi.login as any).mockResolvedValue(sampleLoginResponse);
    (authApi.getMe as any).mockResolvedValue(sampleUserInfo);
    const store = useAuthStore();
    await store.login({
      account: 'alicex',
      password: 'Abc12345',
      captchaId: 'cap-1',
      captchaCode: '1234',
    });

    expect(authApi.getMe).toHaveBeenCalledOnce();
    expect(store.profile?.permissions).toEqual([
      'sys:user:list',
      'sys:user:create',
    ]);
    expect(store.profile?.userName).toBe('张三');
  });

  it('fetchMe populates profile when called independently', async () => {
    (authApi.getMe as any).mockResolvedValue(sampleUserInfo);
    TokenStorage.set('AT');
    const store = useAuthStore();
    await store.fetchMe();
    expect(store.profile?.userAccount).toBe('alicex');
  });

  it('hasPermission returns true when code is in permissions list', () => {
    const store = useAuthStore();
    store.$patch({ profile: sampleUserInfo });
    expect(store.hasPermission('sys:user:list')).toBe(true);
    expect(store.hasPermission('sys:user:delete')).toBe(false);
  });

  it('hasPermission returns false when not logged in', () => {
    const store = useAuthStore();
    expect(store.hasPermission('any')).toBe(false);
  });

  it('ensureProfile dedupes concurrent calls via profileLoading flag', async () => {
    let resolveGetMe: (value: unknown) => void;
    const getMePromise = new Promise((r) => {
      resolveGetMe = r;
    });
    (authApi.getMe as any).mockReturnValueOnce(getMePromise);

    TokenStorage.set('AT');
    const store = useAuthStore();

    // Fire 2 concurrent ensureProfile() calls. The first synchronously
    // enters fetchMe() and sets profileLoading=true before awaiting
    // the mocked getMe; the second caller sees the flag and bails out.
    const call1 = store.ensureProfile();
    const call2 = store.ensureProfile();

    resolveGetMe!(sampleUserInfo);
    await Promise.all([call1, call2]);

    expect(authApi.getMe).toHaveBeenCalledOnce();
    expect(store.profile?.userAccount).toBe('alicex');
    expect(store.profileLoading).toBe(false);
  });

  it('ensureProfile is no-op when not authenticated', async () => {
    localStorage.clear();
    const store = useAuthStore();
    await store.ensureProfile();
    expect(authApi.getMe).not.toHaveBeenCalled();
    expect(store.profile).toBeNull();
  });

  it('ensureProfile is no-op when profile already loaded', async () => {
    TokenStorage.set('AT');
    const store = useAuthStore();
    store.$patch({ profile: sampleUserInfo });
    await store.ensureProfile();
    expect(authApi.getMe).not.toHaveBeenCalled();
  });
});
