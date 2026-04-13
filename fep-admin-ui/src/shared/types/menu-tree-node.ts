/**
 * 菜单状态枚举，对齐后端 com.puchain.fep.web.sysmgmt.menu.domain.MenuStatus。
 */
export type MenuStatus = 'ENABLED' | 'DISABLED';

/**
 * 菜单树节点 DTO，对齐后端
 * com.puchain.fep.web.sysmgmt.menu.dto.MenuTreeNode。
 *
 * <p>用于前端渲染树形菜单结构，children 为递归子节点列表。</p>
 */
export interface MenuTreeNode {
  /** 菜单 ID */
  menuId: string;
  /** 菜单编码 */
  menuCode: string;
  /** 菜单名称 */
  menuName: string;
  /** 父级菜单 ID，根节点可为 null 或空串 */
  parentId: string | null;
  /** 菜单层级（1-based） */
  menuLevel: number;
  /** 菜单图标 */
  menuIcon: string | null;
  /** 排序序号 */
  sortOrder: number;
  /** 菜单状态 */
  menuStatus: MenuStatus;
  /** 组件路径 */
  componentPath: string | null;
  /** 路由路径 */
  routePath: string | null;
  /** 子节点列表 */
  children: MenuTreeNode[];
}
