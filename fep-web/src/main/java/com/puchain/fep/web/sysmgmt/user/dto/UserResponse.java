package com.puchain.fep.web.sysmgmt.user.dto;

import com.puchain.fep.web.common.desensitize.Desensitize;
import com.puchain.fep.web.common.desensitize.DesensitizeType;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应 DTO。
 *
 * <p>手机号做脱敏处理（§8.3 数据掩码）：{@code phone} 字段以
 * {@link Desensitize @Desensitize(PHONE)} 声明，JSON 序列化时经
 * {@code DesensitizeService} 脱敏（保留前 3 后 4）。字段内存中保留明文，
 * 仅在序列化输出时掩蔽。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class UserResponse {

    private String userId;
    private String userAccount;
    private String userName;

    /** 手机号（明文存储，@Desensitize 序列化时脱敏为 138****8000）。 */
    @Desensitize(DesensitizeType.PHONE)
    private String phone;
    private String email;
    private String department;
    private UserStatus userStatus;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;
    private List<String> roleCodes;

    /**
     * 从 SysUser Entity 构建响应 DTO（手机号明文存储，序列化期经 @Desensitize 脱敏）。
     *
     * @param user      用户 Entity
     * @param roleCodes 用户角色编码列表
     * @return 响应 DTO
     */
    public static UserResponse from(final SysUser user, final List<String> roleCodes) {
        UserResponse resp = new UserResponse();
        resp.setUserId(user.getUserId());
        resp.setUserAccount(user.getUserAccount());
        resp.setUserName(user.getUserName());
        resp.setPhone(user.getPhone());
        resp.setEmail(user.getEmail());
        resp.setDepartment(user.getDepartment());
        resp.setUserStatus(user.getUserStatus());
        resp.setLastLoginTime(user.getLastLoginTime());
        resp.setCreateTime(user.getCreateTime());
        resp.setCreateBy(user.getCreateBy());
        resp.setUpdateTime(user.getUpdateTime());
        resp.setUpdateBy(user.getUpdateBy());
        resp.setRoleCodes(roleCodes);
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 获取登录账号。
     *
     * @return 登录账号
     */
    public String getUserAccount() {
        return userAccount;
    }

    /**
     * 获取用户姓名。
     *
     * @return 用户姓名
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 获取手机号（明文；JSON 序列化时经 @Desensitize 脱敏为 138****8000）。
     *
     * @return 明文手机号
     */
    public String getPhone() {
        return phone;
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
     * 获取部门名称。
     *
     * @return 部门名称
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态
     */
    public UserStatus getUserStatus() {
        return userStatus;
    }

    /**
     * 获取最后登录时间。
     *
     * @return 最后登录时间
     */
    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
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
     * 获取创建人。
     *
     * @return 创建人
     */
    public String getCreateBy() {
        return createBy;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 获取更新人。
     *
     * @return 更新人
     */
    public String getUpdateBy() {
        return updateBy;
    }

    /**
     * 获取角色编码列表。
     *
     * @return 角色编码列表
     */
    public List<String> getRoleCodes() {
        return roleCodes;
    }

    // ===== Setters =====

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 设置登录账号。
     *
     * @param userAccount 登录账号
     */
    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
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
     * 设置手机号。
     *
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
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
     * 设置部门名称。
     *
     * @param department 部门名称
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 设置用户状态。
     *
     * @param userStatus 用户状态
     */
    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    /**
     * 设置最后登录时间。
     *
     * @param lastLoginTime 最后登录时间
     */
    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
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
     * 设置创建人。
     *
     * @param createBy 创建人
     */
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 设置更新人。
     *
     * @param updateBy 更新人
     */
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 设置角色编码列表。
     *
     * @param roleCodes 角色编码列表
     */
    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
