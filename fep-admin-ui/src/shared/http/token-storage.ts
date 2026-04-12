/**
 * TokenStorage — access / refresh token persistence.
 *
 * Persists JWT tokens in localStorage under namespaced keys so that the auth
 * store and http interceptors can share state without circular imports.
 */
const ACCESS_KEY = 'fep.token';
const REFRESH_KEY = 'fep.refreshToken';

export const TokenStorage = {
  get(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  },
  set(token: string): void {
    localStorage.setItem(ACCESS_KEY, token);
  },
  getRefresh(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  },
  setRefresh(token: string): void {
    localStorage.setItem(REFRESH_KEY, token);
  },
  clear(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};
