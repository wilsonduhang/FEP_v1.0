import { mount } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { describe, expect, it, beforeEach } from 'vitest';
import ElementPlus from 'element-plus';
import AdminLayout from '../AdminLayout.vue';
import { useAuthStore } from '@/stores/auth';
import type { MenuTreeNode } from '@/shared/types/menu-tree-node';

const routes = [{ path: '/', component: { template: '<div/>' } }];

describe('AdminLayout menu rendering', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  function buildNode(overrides: Partial<MenuTreeNode> = {}): MenuTreeNode {
    return {
      menuId: '',
      menuCode: '',
      menuName: '',
      parentId: '',
      menuLevel: 1,
      menuIcon: '',
      sortOrder: 0,
      menuStatus: 'ENABLED',
      componentPath: '',
      routePath: '',
      children: [],
      ...overrides,
    };
  }

  it('renders "首页" fallback when menuTree is empty', async () => {
    const store = useAuthStore();
    store.profile = {
      userId: '1',
      userAccount: 'admin1',
      userName: 'Admin',
      phone: null,
      email: null,
      department: null,
      roleCodes: [],
      permissions: [],
      menuTree: [],
    };
    const router = createRouter({ history: createMemoryHistory(), routes });
    router.push('/');
    await router.isReady();
    const wrapper = mount(AdminLayout, { global: { plugins: [router, ElementPlus] } });
    expect(wrapper.text()).toContain('首页');
  });

  it('renders leaf menu items from menuTree', async () => {
    const store = useAuthStore();
    store.profile = {
      userId: '1',
      userAccount: 'admin1',
      userName: 'Admin',
      phone: '',
      email: '',
      department: '',
      roleCodes: [],
      permissions: [],
      menuTree: [
        buildNode({
          menuId: 'm1',
          menuCode: 'ENT_QUERY',
          menuName: '企业查询',
          children: [
            buildNode({
              menuId: 'm1a',
              menuCode: 'ENT_QUERY_TASK',
              menuName: '查询任务管理',
              routePath: '/enterprise/query-tasks',
              parentId: 'm1',
              menuLevel: 2,
            }),
          ],
        }),
      ],
    };
    const router = createRouter({ history: createMemoryHistory(), routes });
    router.push('/');
    await router.isReady();
    const wrapper = mount(AdminLayout, { global: { plugins: [router, ElementPlus] } });
    // el-sub-menu renders title visibly but children may be in collapsed popup;
    // check html() which includes all DOM including hidden portions
    expect(wrapper.html()).toContain('企业查询');
    expect(wrapper.html()).toContain('查询任务管理');
  });
});
