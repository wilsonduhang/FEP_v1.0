import type { RouteRecordRaw } from 'vue-router';

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      { path: '', name: 'Login', component: () => import('@/features/auth/views/LoginPage.vue') },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: () => import('@/features/home/views/HomePage.vue') },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
];
