import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './routes';
import { TokenStorage } from '@/shared/http/token-storage';

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authed = TokenStorage.get() !== null;
  if (to.meta.requiresAuth && !authed) {
    return { name: 'Login', query: { redirect: to.fullPath } };
  }
  if (to.name === 'Login' && authed) {
    return { name: 'Home' };
  }
  return true;
});

export default router;
