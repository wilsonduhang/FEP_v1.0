package com.puchain.fep.web.sysmgmt.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 角色更新请求 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称最长 100 字符")
    private String roleName;

    @Size(max = 500, message = "数据权限范围最长 500 字符")
    private String dataScope;

    @Size(max = 500, message = "备注最长 500 字符")
    private String remark;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
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
