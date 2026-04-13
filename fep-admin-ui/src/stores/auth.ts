import { defineStore } from 'pinia';
import { TokenStorage } from '@/shared/http/token-storage';
import {
  authApi,
  type LoginRequest,
  type UserInfoResponse,
} from '@/features/auth/api/auth-api';

/**
 * Flat user profile persisted in store after login.
 * Fields align 1:1 with backend UserInfoResponse (P6e.2): includes
 * permissions list and authorized menu tree for front-end gating.
 */
export type UserProfile = UserInfoResponse;

interface AuthState {
  profile: UserProfile | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({ profile: null }),
  getters: {
    isAuthenticated: (): boolean => TokenStorage.get() !== null,
  },
  actions: {
    /**
     * Perform login then immediately fetch full user profile so that
     * permissions and menu tree are available before router navigation.
     */
    async login(payload: LoginRequest) {
      const response = await authApi.login(payload);
      TokenStorage.set(response.accessToken);
      TokenStorage.setRefresh(response.refreshToken);
      await this.fetchMe();
    },
    /**
     * Retrieve current user info from GET /auth/me and cache in store.
     * Used both after login and on page reload when token still valid.
     */
    async fetchMe() {
      const info = await authApi.getMe();
      this.profile = info;
    },
    async logout() {
      try {
        await authApi.logout();
      } catch {
        // Silent: clear local state regardless of backend logout result.
      }
      this.localLogout();
    },
    localLogout() {
      TokenStorage.clear();
      this.profile = null;
    },
    /**
     * Declarative permission check against cached permissions list.
     * Returns false when not logged in or the code is absent.
     */
    hasPermission(code: string): boolean {
      return this.profile?.permissions.includes(code) ?? false;
    },
    bindExpiryListener() {
      window.addEventListener('auth:expired', () => this.localLogout());
    },
  },
});
