import { describe, it, expect, beforeEach } from 'vitest';
import { createRouter, createMemoryHistory } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';
import { TokenStorage } from '@/shared/http/token-storage';

/**
 * Minimal in-memory router used for isolated guard unit tests. We intentionally
 * do NOT import the production router singleton — instantiating a fresh router
 * per test avoids cross-test state leakage and singleton initialization issues
 * (pinia, history state). The guard logic below must mirror the production
 * guard in `src/router/index.ts`; if one changes, update the other.
 */
function makeTestRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'Login', component: { template: '<div>login</div>' } },
      {
        path: '/home',
        name: 'Home',
        component: { template: '<div>home</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/admin',
        name: 'Admin',
        component: { template: '<div>admin</div>' },
        meta: { requiresAuth: true, permission: 'sys:admin:access' },
      },
    ],
  });
  router.beforeEach((to) => {
    const store = useAuthStore();
    const authed = TokenStorage.get() !== null;
    if (to.meta.requiresAuth && !authed) {
      return { name: 'Login', query: { redirect: to.fullPath } };
    }
    if (to.name === 'Login' && authed) {
      return { name: 'Home' };
    }
    if (to.meta.requiresAuth && to.meta.permission && !store.hasPermission(to.meta.permission)) {
      return { name: 'Home', query: { forbidden: to.fullPath } };
    }
    return true;
  });
  return router;
}

describe('router guards', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('unauth → home redirects to login with redirect query', async () => {
    const router = makeTestRouter();
    await router.push('/home');
    expect(router.currentRoute.value.name).toBe('Login');
    expect(router.currentRoute.value.query.redirect).toBe('/home');
  });

  it('authed → login redirects to home', async () => {
    TokenStorage.set('AT');
    const router = makeTestRouter();
    await router.push('/login');
    expect(router.currentRoute.value.name).toBe('Home');
  });

  it('authed + has permission → admin passes', async () => {
    TokenStorage.set('AT');
    const router = makeTestRouter();
    const store = useAuthStore();
    store.$patch({
      profile: {
        userId: 'u1',
        userAccount: 'a',
        userName: 'a',
        phone: null,
        email: null,
        department: null,
        roleCodes: ['ADMIN'],
        permissions: ['sys:admin:access'],
        menuTree: [],
      },
    });
    await router.push('/admin');
    expect(router.currentRoute.value.name).toBe('Admin');
  });

  it('authed without permission → admin redirects to home with forbidden query', async () => {
    TokenStorage.set('AT');
    const router = makeTestRouter();
    const store = useAuthStore();
    store.$patch({
      profile: {
        userId: 'u1',
        userAccount: 'a',
        userName: 'a',
        phone: null,
        email: null,
        department: null,
        roleCodes: ['USER'],
        permissions: ['sys:user:list'],
        menuTree: [],
      },
    });
    await router.push('/admin');
    expect(router.currentRoute.value.name).toBe('Home');
    expect(router.currentRoute.value.query.forbidden).toBe('/admin');
  });
});
