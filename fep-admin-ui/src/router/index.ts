import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';
import { TokenStorage } from '@/shared/http/token-storage';
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * Global navigation guard.
 *
 * Order of checks (fail-fast):
 *  1. `requiresAuth` + unauth → redirect to /login with `redirect` query.
 *  2. Already authed visiting /login → redirect to /home.
 *  3. `requiresAuth` + `permission` + missing permission →
 *     redirect to /home with `forbidden=<path>` query (NOT /login, to avoid loops).
 *
 * The `useAuthStore()` call is intentionally inside the callback so that pinia
 * is guaranteed to be installed on the app instance when the first navigation
 * fires (pinia is registered in `main.ts` before `app.use(router)`).
 */
router.beforeEach((to) => {
  const authed = TokenStorage.get() !== null;

  if (to.meta.requiresAuth && !authed) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }
  if (to.name === 'Login' && authed) {
    return { name: 'Home' };
  }
  if (to.meta.requiresAuth && to.meta.permission) {
    const store = useAuthStore();
    if (!store.hasPermission(to.meta.permission)) {
      return { name: 'Home', query: { forbidden: to.fullPath } };
    }
  }
  return true;
});

export default router;
