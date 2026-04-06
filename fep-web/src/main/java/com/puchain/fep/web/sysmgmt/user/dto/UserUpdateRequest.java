package com.puchain.fep.web.sysmgmt.user.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 用户更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.1 用户管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class UserUpdateRequest {

    @Size(max = 100, message = "用户姓名最长 100 字符")
    private String userName;

    @Size(max = 20, message = "手机号最长 20 字符")
    private String phone;

    @Size(max = 100, message = "邮箱最长 100 字符")
    private String email;

    @Size(max = 100, message = "部门名称最长 100 字符")
    private String department;

    private List<String> roleIds;

    /**
     * 获取用户姓名。
     *
     * @return 用户姓名
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 设置用户姓名。
     *
     * @param userName 用户姓名
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获取手机号。
     *
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号。
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取邮箱。
     *
     * @return 邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱。
     *
     * @param email 邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取部门名称。
     *
     * @return 部门名称
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 设置部门名称。
     *
     * @param department 部门名称
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 获取角色 ID 列表。
     *
     * @return 角色 ID 列表
     */
    public List<String> getRoleIds() {
        return roleIds;
    }

    /**
     * 设置角色 ID 列表。
     *
     * @param roleIds 角色 ID 列表
     */
    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }
}
