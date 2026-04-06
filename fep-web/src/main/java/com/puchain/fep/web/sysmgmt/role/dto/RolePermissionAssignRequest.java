package com.puchain.fep.web.sysmgmt.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色权限分配请求 DTO。
 *
 * <p>将一组菜单权限码分配给指定角色，替换原有权限。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RolePermissionAssignRequest {

    @NotNull(message = "权限列表不能为 null")
    @Valid
    private List<MenuPermission> permissions;

    /**
     * 菜单权限项。
     */
    public static class MenuPermission {

        @NotNull(message = "菜单 ID 不能为空")
        private String menuId;

        @NotEmpty(message = "权限码列表不能为空")
        private List<String> permissionCodes;

        public String getMenuId() {
            return menuId;
        }

        public void setMenuId(String menuId) {
            this.menuId = menuId;
        }

        public List<String> getPermissionCodes() {
            return permissionCodes;
        }

        public void setPermissionCodes(List<String> permissionCodes) {
            this.permissionCodes = permissionCodes;
        }
    }

    public List<MenuPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<MenuPermission> permissions) {
        this.permissions = permissions;
    }
}
