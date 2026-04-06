package com.puchain.fep.web.auth.domain;

import java.util.Collections;
import java.util.List;

/**
 * 登录响应 DTO。
 *
 * <p>登录成功后返回 access/refresh token、用户信息、角色列表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String userId;
    private final String userAccount;
    private final String userName;
    private final List<String> roleCodes;
    private final boolean passwordChangeRequired;

    /**
     * 构造登录响应。
     *
     * @param accessToken            Access Token
     * @param refreshToken           Refresh Token
     * @param userId                 用户 ID
     * @param userAccount            登录账号
     * @param userName               用户姓名
     * @param roleCodes              角色编码列表
     * @param passwordChangeRequired 是否需要修改密码
     */
    public LoginResponse(final String accessToken, final String refreshToken,
                         final String userId, final String userAccount,
                         final String userName, final List<String> roleCodes,
                         final boolean passwordChangeRequired) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.userAccount = userAccount;
        this.userName = userName;
        this.roleCodes = roleCodes != null ? Collections.unmodifiableList(roleCodes) : List.of();
        this.passwordChangeRequired = passwordChangeRequired;
    }

    /**
     * 获取 Access Token。
     *
     * @return JWT access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * 获取 Refresh Token。
     *
     * @return JWT refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 UUID
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
     * 获取角色编码列表。
     *
     * @return 角色编码
     */
    public List<String> getRoleCodes() {
        return roleCodes;
    }

    /**
     * 是否需要修改密码（首次登录或过期）。
     *
     * @return true 需要修改
     */
    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }
}
