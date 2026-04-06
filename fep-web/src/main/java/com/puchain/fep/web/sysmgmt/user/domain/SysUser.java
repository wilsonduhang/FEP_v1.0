package com.puchain.fep.web.sysmgmt.user.domain;

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
 * 用户信息 Entity，映射 t_sys_user 表。
 *
 * <p>参见 PRD v1.3 §6.4 + P6a.1 字段补充决策。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_user")
@EntityListeners(AuditingEntityListener.class)
public class SysUser {

    @Id
    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "user_account", nullable = false, length = 50, unique = true)
    private String userAccount;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "department", length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_password_change_time")
    private LocalDateTime lastPasswordChangeTime;

    @Column(name = "password_history", length = 1024)
    private String passwordHistory;

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
    public SysUser() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取用户唯一标识。
     *
     * @return 用户 ID (UUID 32位)
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
     * 获取密码散列值。
     *
     * @return BCrypt 散列字符串
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 获取手机号。
     *
     * @return 手机号，可能为 null
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 获取邮箱。
     *
     * @return 邮箱，可能为 null
     */
    public String getEmail() {
        return email;
    }

    /**
     * 获取所属部门。
     *
     * @return 部门名称，可能为 null
     */
    public String getDepartment() {
        return department;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态枚举
     */
    public UserStatus getUserStatus() {
        return userStatus;
    }

    /**
     * 获取锁定到期时间。
     *
     * @return 锁定到期时间，未锁定时为 null
     */
    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    /**
     * 获取连续登录失败次数。
     *
     * @return 失败次数
     */
    public Integer getLoginFailCount() {
        return loginFailCount;
    }

    /**
     * 获取最后登录时间。
     *
     * @return 最后登录时间，可能为 null
     */
    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    /**
     * 获取密码最近变更时间。
     *
     * @return 密码变更时间，可能为 null
     */
    public LocalDateTime getLastPasswordChangeTime() {
        return lastPasswordChangeTime;
    }

    /**
     * 获取密码历史记录。
     *
     * @return 最近三次密码散列（JSON 数组），可能为 null
     */
    public String getPasswordHistory() {
        return passwordHistory;
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
     * @return 创建人账号
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
     * @return 更新人账号
     */
    public String getUpdateBy() {
        return updateBy;
    }

    // ===== Setters =====

    /**
     * 设置用户唯一标识。
     *
     * @param userId 用户 ID (UUID 32位)
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
     * 设置密码散列值。
     *
     * @param passwordHash BCrypt 散列字符串
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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
     * 设置所属部门。
     *
     * @param department 部门名称
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * 设置用户状态。
     *
     * @param userStatus 用户状态枚举
     */
    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    /**
     * 设置锁定到期时间。
     *
     * @param lockedUntil 锁定到期时间
     */
    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    /**
     * 设置连续登录失败次数。
     *
     * @param loginFailCount 失败次数
     */
    public void setLoginFailCount(Integer loginFailCount) {
        this.loginFailCount = loginFailCount;
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
     * 设置密码最近变更时间。
     *
     * @param lastPasswordChangeTime 密码变更时间
     */
    public void setLastPasswordChangeTime(LocalDateTime lastPasswordChangeTime) {
        this.lastPasswordChangeTime = lastPasswordChangeTime;
    }

    /**
     * 设置密码历史记录。
     *
     * @param passwordHistory 最近三次密码散列（JSON 数组）
     */
    public void setPasswordHistory(String passwordHistory) {
        this.passwordHistory = passwordHistory;
    }
}
