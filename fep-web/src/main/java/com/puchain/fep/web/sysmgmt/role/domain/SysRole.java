package com.puchain.fep.web.sysmgmt.role.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 角色信息 Entity，映射 t_sys_role 表。
 *
 * <p>参见 PRD v1.3 §6.4 角色信息表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_role")
@EntityListeners(AuditingEntityListener.class)
public class SysRole {

    @Id
    @Column(name = "role_id", length = 32)
    private String roleId;

    @Column(name = "role_code", nullable = false, length = 50, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private RoleType roleType;

    @Column(name = "data_scope", length = 500)
    private String dataScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_status", nullable = false, length = 20)
    private RoleStatus roleStatus;

    @Column(name = "remark", length = 500)
    private String remark;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @CreatedBy
    @Column(name = "create_by", length = 50, updatable = false)
    private String createBy;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @LastModifiedBy
    @Column(name = "update_by", length = 50)
    private String updateBy;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysRole() {
        /* for JPA */
    }

    // ===== Getters =====

    public String getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public String getDataScope() {
        return dataScope;
    }

    public RoleStatus getRoleStatus() {
        return roleStatus;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    // ===== Setters =====

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public void setRoleStatus(RoleStatus roleStatus) {
        this.roleStatus = roleStatus;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
