package com.puchain.fep.web.sysmgmt.menu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 菜单配置 Entity，映射 t_sys_menu 表。
 *
 * <p>支持 3 层树形结构（parent_id 自引用）。
 * 参见 PRD v1.3 §6.4 菜单配置表 + §5.10.3 菜单管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_menu")
public class SysMenu {

    @Id
    @Column(name = "menu_id", length = 32)
    private String menuId;

    @Column(name = "menu_code", nullable = false, length = 50, unique = true)
    private String menuCode;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "parent_id", length = 32)
    private String parentId;

    @Column(name = "menu_level", nullable = false)
    private Integer menuLevel;

    @Column(name = "menu_icon", length = 100)
    private String menuIcon;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "menu_status", nullable = false, length = 20)
    private MenuStatus menuStatus;

    @Column(name = "component_path", length = 200)
    private String componentPath;

    @Column(name = "route_path", length = 200)
    private String routePath;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysMenu() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取菜单唯一标识。
     *
     * @return 菜单 ID (UUID 32位)
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
     * @return 父级菜单 ID，顶级为 null
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * 获取菜单层级。
     *
     * @return 菜单层级 (1-3)
     */
    public Integer getMenuLevel() {
        return menuLevel;
    }

    /**
     * 获取菜单图标。
     *
     * @return 图标 class，可能为 null
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
     * @return 菜单状态枚举
     */
    public MenuStatus getMenuStatus() {
        return menuStatus;
    }

    /**
     * 获取前端组件路径。
     *
     * @return 组件路径，可能为 null
     */
    public String getComponentPath() {
        return componentPath;
    }

    /**
     * 获取前端路由路径。
     *
     * @return 路由路径，可能为 null
     */
    public String getRoutePath() {
        return routePath;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置菜单唯一标识。
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
     * @param menuLevel 菜单层级 (1-3)
     */
    public void setMenuLevel(Integer menuLevel) {
        this.menuLevel = menuLevel;
    }

    /**
     * 设置菜单图标。
     *
     * @param menuIcon 图标 class
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
     * @param menuStatus 菜单状态枚举
     */
    public void setMenuStatus(MenuStatus menuStatus) {
        this.menuStatus = menuStatus;
    }

    /**
     * 设置前端组件路径。
     *
     * @param componentPath 组件路径
     */
    public void setComponentPath(String componentPath) {
        this.componentPath = componentPath;
    }

    /**
     * 设置前端路由路径。
     *
     * @param routePath 路由路径
     */
    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
