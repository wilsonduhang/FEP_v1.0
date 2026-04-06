package com.puchain.fep.web.sysmgmt.menu.dto;

import com.puchain.fep.web.sysmgmt.menu.domain.MenuStatus;
import com.puchain.fep.web.sysmgmt.menu.domain.SysMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 DTO。
 *
 * <p>用于返回前端的树形菜单结构，包含递归子节点列表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MenuTreeNode {

    private String menuId;
    private String menuCode;
    private String menuName;
    private String parentId;
    private Integer menuLevel;
    private String menuIcon;
    private Integer sortOrder;
    private MenuStatus menuStatus;
    private String componentPath;
    private String routePath;
    private List<MenuTreeNode> children;

    /**
     * 从 SysMenu Entity 构建树节点（不含子节点）。
     *
     * @param menu 菜单 Entity
     * @return 树节点 DTO
     */
    public static MenuTreeNode from(final SysMenu menu) {
        MenuTreeNode node = new MenuTreeNode();
        node.setMenuId(menu.getMenuId());
        node.setMenuCode(menu.getMenuCode());
        node.setMenuName(menu.getMenuName());
        node.setParentId(menu.getParentId());
        node.setMenuLevel(menu.getMenuLevel());
        node.setMenuIcon(menu.getMenuIcon());
        node.setSortOrder(menu.getSortOrder());
        node.setMenuStatus(menu.getMenuStatus());
        node.setComponentPath(menu.getComponentPath());
        node.setRoutePath(menu.getRoutePath());
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * 添加子节点。
     *
     * @param child 子节点
     */
    public void addChild(final MenuTreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }

    // ===== Getters =====

    /**
     * 获取菜单 ID。
     *
     * @return 菜单 ID
     */
    public String getMenuId() {
        return menuId;
    }

    /**
     * 获取菜单编码。
     *
     * @return 菜单编码
     */
    public String getMenuCode() {
        return menuCode;
    }

    /**
     * 获取菜单名称。
     *
     * @return 菜单名称
     */
    public String getMenuName() {
        return menuName;
    }

    /**
     * 获取父级菜单 ID。
     *
     * @return 父级菜单 ID
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * 获取菜单层级。
     *
     * @return 菜单层级
     */
    public Integer getMenuLevel() {
        return menuLevel;
    }

    /**
     * 获取菜单图标。
     *
     * @return 菜单图标
     */
    public String getMenuIcon() {
        return menuIcon;
    }

    /**
     * 获取排序序号。
     *
     * @return 排序序号
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取菜单状态。
     *
     * @return 菜单状态
     */
    public MenuStatus getMenuStatus() {
        return menuStatus;
    }

    /**
     * 获取组件路径。
     *
     * @return 组件路径
     */
    public String getComponentPath() {
        return componentPath;
    }

    /**
     * 获取路由路径。
     *
     * @return 路由路径
     */
    public String getRoutePath() {
        return routePath;
    }

    /**
     * 获取子节点列表。
     *
     * @return 子节点列表
     */
    public List<MenuTreeNode> getChildren() {
        return children;
    }

    // ===== Setters =====

    /**
     * 设置菜单 ID。
     *
     * @param menuId 菜单 ID
     */
    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    /**
     * 设置菜单编码。
     *
     * @param menuCode 菜单编码
     */
    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    /**
     * 设置菜单名称。
     *
     * @param menuName 菜单名称
     */
    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    /**
     * 设置父级菜单 ID。
     *
     * @param parentId 父级菜单 ID
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * 设置菜单层级。
     *
     * @param menuLevel 菜单层级
     */
    public void setMenuLevel(Integer menuLevel) {
        this.menuLevel = menuLevel;
    }

    /**
     * 设置菜单图标。
     *
     * @param menuIcon 菜单图标
     */
    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }

    /**
     * 设置排序序号。
     *
     * @param sortOrder 排序序号
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置菜单状态。
     *
     * @param menuStatus 菜单状态
     */
    public void setMenuStatus(MenuStatus menuStatus) {
        this.menuStatus = menuStatus;
    }

    /**
     * 设置组件路径。
     *
     * @param componentPath 组件路径
     */
    public void setComponentPath(String componentPath) {
        this.componentPath = componentPath;
    }

    /**
     * 设置路由路径。
     *
     * @param routePath 路由路径
     */
    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    /**
     * 设置子节点列表。
     *
     * @param children 子节点列表
     */
    public void setChildren(List<MenuTreeNode> children) {
        this.children = children;
    }
}
