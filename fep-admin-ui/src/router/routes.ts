import type { RouteRecordRaw } from 'vue-router';

/**
 * Declarative route meta extension consumed by the global `beforeEach` guard
 * in `src/router/index.ts`. Routes that require authentication set
 * `requiresAuth: true`; routes that additionally require a permission code
 * set `permission: 'sys:xxx:yyy'` and the guard checks it via
 * `useAuthStore().hasPermission()`.
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    permission?: string;
  }
}

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
      {
        path: 'enterprise/query-tasks',
        name: 'QueryTasks',
        component: () => import('@/features/ent-query/views/QueryTasksPage.vue'),
      },
      {
        path: 'enterprise/auth-letters',
        name: 'AuthLetters',
        component: () => import('@/features/ent-query/views/AuthLettersPage.vue'),
      },
      {
        path: 'biz/definitions',
        name: 'MessageDefinitions',
        component: () => import('@/features/biz-data/views/MessageDefinitionsPage.vue'),
      },
      {
        path: 'biz/records',
        name: 'MessageRecords',
        component: () => import('@/features/biz-data/views/MessageRecordsPage.vue'),
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
];
