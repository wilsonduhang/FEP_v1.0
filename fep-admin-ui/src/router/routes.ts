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
      {
        path: 'submit/dashboard',
        name: 'SubmissionDashboard',
        component: () => import('@/features/submission/views/SubmissionDashboardPage.vue'),
      },
      {
        path: 'submit/output-interfaces',
        name: 'OutputInterfaces',
        component: () => import('@/features/submission/views/OutputInterfacesPage.vue'),
      },
      {
        path: 'submit/data-sources',
        name: 'DataSources',
        component: () => import('@/features/submission/views/DataSourcesPage.vue'),
      },
      {
        path: 'submit/scenes',
        name: 'BusinessScenes',
        component: () => import('@/features/submission/views/BusinessScenesPage.vue'),
      },
      {
        path: 'submit/message-summary',
        name: 'MessageSummary',
        component: () => import('@/features/submission/views/MessageSummaryPage.vue'),
      },
      {
        path: 'report/upload',
        name: 'ReportUpload',
        component: () => import('@/features/submission/views/ReportUploadPage.vue'),
      },
      {
        path: 'report/records',
        name: 'ReportRecords',
        component: () => import('@/features/submission/views/ReportRecordsPage.vue'),
      },
      {
        path: 'report/view',
        name: 'ReportView',
        component: () => import('@/features/submission/views/ReportViewPage.vue'),
      },
      {
        path: 'report/push',
        name: 'ReportPush',
        component: () => import('@/features/submission/views/ReportPushPage.vue'),
      },
      {
        path: 'tlq/nodes',
        name: 'TlqNodes',
        component: () => import('@/features/tlq/views/TlqNodesPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'tlq/queues',
        name: 'TlqQueues',
        component: () => import('@/features/tlq/views/TlqQueuesPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'tlq/connectivity',
        name: 'TlqConnectivity',
        component: () => import('@/features/tlq/views/TlqConnectivityPage.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'system/config/dir-map',
        name: 'SysDirMapConfig',
        component: () => import('@/features/sys-config/views/DirMapConfigPage.vue'),
        // NO meta.permission — feedback_permission_code_vs_menu_code 红线：
        // menuTree 后端过滤是权限的唯一权威源；路由 meta 仅声明 requiresAuth + title。
        meta: { requiresAuth: true, title: '报文方向映射' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
];
