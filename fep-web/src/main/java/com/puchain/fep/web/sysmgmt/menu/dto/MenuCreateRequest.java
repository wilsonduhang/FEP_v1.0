package com.puchain.fep.web.sysmgmt.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 菜单创建请求 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MenuCreateRequest {

    @NotBlank(message = "菜单编码不能为空")
    @Size(max = 50, message = "菜单编码最长 50 字符")
    private String menuCode;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 100, message = "菜单名称最长 100 字符")
    private String menuName;

    @Size(max = 32, message = "父级菜单 ID 最长 32 字符")
    private String parentId;

    @NotNull(message = "菜单层级不能为空")
    @Min(value = 1, message = "菜单层级最小为 1")
    @Max(value = 3, message = "菜单层级最大为 3")
    private Integer menuLevel;

    @Size(max = 100, message = "菜单图标最长 100 字符")
    private String menuIcon;

    @NotNull(message = "排序序号不能为空")
    private Integer sortOrder;

    @Size(max = 200, message = "组件路径最长 200 字符")
    private String componentPath;

    @Size(max = 200, message = "路由路径最长 200 字符")
    private String routePath;

    /**
     * 获取菜单编码。
     *
     * @return 菜单编码
     */
    public String getMenuCode() {
        return menuCode;
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
     * 获取菜单名称。
     *
     * @return 菜单名称
     */
    public String getMenuName() {
        return menuName;
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
     * 获取父级菜单 ID。
     *
     * @return 父级菜单 ID
     */
    public String getParentId() {
        return parentId;
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
     * 获取菜单层级。
     *
     * @return 菜单层级
     */
    public Integer getMenuLevel() {
        return menuLevel;
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
     * 获取菜单图标。
     *
     * @return 菜单图标
     */
    public String getMenuIcon() {
        return menuIcon;
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
     * 获取排序序号。
     *
     * @return 排序序号
     */
    public Integer getSortOrder() {
        return sortOrder;
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
     * 获取组件路径。
     *
     * @return 组件路径
     */
    public String getComponentPath() {
        return componentPath;
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
     * 获取路由路径。
     *
     * @return 路由路径
     */
    public String getRoutePath() {
        return routePath;
    }

    /**
     * 设置路由路径。
     *
     * @param routePath 路由路径
     */
    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }
}
