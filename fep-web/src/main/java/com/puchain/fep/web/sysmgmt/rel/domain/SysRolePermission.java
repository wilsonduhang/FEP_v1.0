package com.puchain.fep.web.sysmgmt.rel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 角色-权限关联 Entity，映射 t_sys_role_permission 表。
 *
 * <p>参见 PRD v1.3 §6.4 角色权限关联表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_role_permission")
public class SysRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "role_id", nullable = false, length = 32)
    private String roleId;

    @Column(name = "menu_id", nullable = false, length = 32)
    private String menuId;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysRolePermission() {
        /* for JPA */
    }

    /**
     * 构造角色-权限关联。
     *
     * @param roleId         角色 ID
     * @param menuId         菜单 ID
     * @param permissionCode 权限码
     */
    public SysRolePermission(final String roleId, final String menuId,
                             final String permissionCode) {
        this.roleId = roleId;
        this.menuId = menuId;
        this.permissionCode = permissionCode;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getMenuId() {
        return menuId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setMenuId(String menuId) {
        this.menuId = menuId;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
