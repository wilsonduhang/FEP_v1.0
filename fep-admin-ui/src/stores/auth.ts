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
  /**
   * In-flight flag for fetchMe() / ensureProfile() concurrency dedupe.
   * Set to true at the start of fetchMe (synchronously, before the
   * await on authApi.getMe()), reset in the finally block. JavaScript
   * single-threaded semantics guarantee that any concurrent caller
   * reaching ensureProfile() after the first caller has entered
   * fetchMe() will see profileLoading === true and bail out.
   */
  profileLoading: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    profile: null,
    profileLoading: false,
  }),
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
     * Sets profileLoading synchronously before awaiting the API call so
     * that concurrent ensureProfile() callers can dedupe on the flag.
     */
    async fetchMe() {
      this.profileLoading = true;
      try {
        this.profile = await authApi.getMe();
      } finally {
        this.profileLoading = false;
      }
    },
    /**
     * Lazy profile hydration for browser refresh scenarios.
     * If the user has a valid token but profile is null (Pinia state
     * lost on page reload), fetch it. Otherwise no-op. Deduped via
     * profileLoading flag to handle concurrent router navigations.
     *
     * Introduced in P7.0.2 (R1 fix from P7.1 E2E browser testing).
     */
    async ensureProfile() {
      if (!this.isAuthenticated || this.profile !== null || this.profileLoading) {
        return;
      }
      await this.fetchMe();
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
      this.profileLoading = false;
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
