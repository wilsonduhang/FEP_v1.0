import { defineStore } from 'pinia';
import { TokenStorage } from '@/shared/http/token-storage';
import { authApi, type LoginRequest, type LoginResponse } from '@/features/auth/api/auth-api';

/** 登录后本地缓存的用户画像，字段与后端 LoginResponse 扁平一一对应（剔除 token）。 */
export type UserProfile = Omit<LoginResponse, 'accessToken' | 'refreshToken'>;

interface AuthState {
  profile: UserProfile | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ profile: null }),
  getters: {
    isAuthenticated: (): boolean => TokenStorage.get() !== null,
  },
  actions: {
    async login(payload: LoginRequest) {
      const response = await authApi.login(payload);
      TokenStorage.set(response.accessToken);
      TokenStorage.setRefresh(response.refreshToken);
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { accessToken: _at, refreshToken: _rt, ...profile } = response;
      this.profile = profile;
    },
    async logout() {
      try {
        await authApi.logout();
      } catch {
        // 静默失败：即使后端 logout 失败也要清理本地状态
      }
      this.localLogout();
    },
    localLogout() {
      TokenStorage.clear();
      this.profile = null;
    },
    bindExpiryListener() {
      window.addEventListener('auth:expired', () => this.localLogout());
    },
  },
});
