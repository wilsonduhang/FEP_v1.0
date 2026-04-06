package com.puchain.fep.web.sysmgmt.role.dto;

import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 角色创建请求 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码最长 50 字符")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称最长 100 字符")
    private String roleName;

    @NotNull(message = "角色类型不能为空")
    private RoleType roleType;

    @Size(max = 500, message = "数据权限范围最长 500 字符")
    private String dataScope;

    @Size(max = 500, message = "备注最长 500 字符")
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
