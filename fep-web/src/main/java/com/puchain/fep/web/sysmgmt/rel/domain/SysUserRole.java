package com.puchain.fep.web.sysmgmt.rel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 用户-角色关联 Entity，映射 t_sys_user_role 表。
 *
 * <p>参见 PRD v1.3 §6.4 用户角色关联表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_user_role")
public class SysUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Column(name = "role_id", nullable = false, length = 32)
    private String roleId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysUserRole() {
        /* for JPA */
    }

    /**
     * 构造用户-角色关联。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    public SysUserRole(final String userId, final String roleId) {
        this.userId = userId;
        this.roleId = roleId;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
