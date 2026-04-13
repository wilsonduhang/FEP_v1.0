import { describe, it, expect } from 'vitest';
import type { MenuTreeNode, MenuStatus } from '../menu-tree-node';

describe('MenuTreeNode', () => {
  it('accepts leaf node without children', () => {
    const leaf: MenuTreeNode = {
      menuId: 'M001',
      menuCode: 'DASHBOARD',
      menuName: '首页',
      parentId: null,
      menuLevel: 1,
      menuIcon: 'icon-home',
      sortOrder: 1,
      menuStatus: 'ENABLED',
      componentPath: 'views/dashboard/Index.vue',
      routePath: '/dashboard',
      children: [],
    };
    expect(leaf.children).toHaveLength(0);
    expect(leaf.menuStatus).toBe('ENABLED');
  });

  it('accepts nested tree and preserves parent-child linkage', () => {
    const child: MenuTreeNode = {
      menuId: 'M011',
      menuCode: 'SYS_USER',
      menuName: '用户管理',
      parentId: 'M010',
      menuLevel: 2,
      menuIcon: null,
      sortOrder: 1,
      menuStatus: 'ENABLED',
      componentPath: 'views/sysmgmt/user/List.vue',
      routePath: '/sysmgmt/user',
      children: [],
    };
    const root: MenuTreeNode = {
      menuId: 'M010',
      menuCode: 'SYSMGMT',
      menuName: '系统管理',
      parentId: null,
      menuLevel: 1,
      menuIcon: 'icon-setting',
      sortOrder: 10,
      menuStatus: 'ENABLED',
      componentPath: null,
      routePath: '/sysmgmt',
      children: [child],
    };
    expect(root.children).toHaveLength(1);
    expect(root.children[0].parentId).toBe(root.menuId);
    expect(root.children[0].menuLevel).toBe(2);
  });

  it('MenuStatus narrows to ENABLED | DISABLED', () => {
    const enabled: MenuStatus = 'ENABLED';
    const disabled: MenuStatus = 'DISABLED';
    expect(enabled).toBe('ENABLED');
    expect(disabled).toBe('DISABLED');
  });
});
