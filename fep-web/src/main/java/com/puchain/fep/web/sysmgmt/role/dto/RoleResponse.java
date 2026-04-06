package com.puchain.fep.web.sysmgmt.role.dto;

import com.puchain.fep.web.sysmgmt.role.domain.RoleStatus;
import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;

import java.time.LocalDateTime;

/**
 * 角色响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RoleResponse {

    private String roleId;
    private String roleCode;
    private String roleName;
    private RoleType roleType;
    private String dataScope;
    private RoleStatus roleStatus;
    private String remark;
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;

    /**
     * 从 SysRole Entity 构建响应 DTO。
     *
     * @param role 角色 Entity
     * @return 响应 DTO
     */
    public static RoleResponse from(final SysRole role) {
        RoleResponse resp = new RoleResponse();
        resp.setRoleId(role.getRoleId());
        resp.setRoleCode(role.getRoleCode());
        resp.setRoleName(role.getRoleName());
        resp.setRoleType(role.getRoleType());
        resp.setDataScope(role.getDataScope());
        resp.setRoleStatus(role.getRoleStatus());
        resp.setRemark(role.getRemark());
        resp.setCreateTime(role.getCreateTime());
        resp.setCreateBy(role.getCreateBy());
        resp.setUpdateTime(role.getUpdateTime());
        resp.setUpdateBy(role.getUpdateBy());
        return resp;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public RoleStatus getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(RoleStatus roleStatus) {
        this.roleStatus = roleStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
