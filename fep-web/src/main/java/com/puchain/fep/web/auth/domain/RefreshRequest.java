package com.puchain.fep.web.auth.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * Token 刷新请求 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RefreshRequest {

    /** Refresh Token。 */
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

    /**
     * 获取 Refresh Token。
     *
     * @return refresh token 字符串
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 设置 Refresh Token。
     *
     * @param refreshToken refresh token 字符串
     */
    public void setRefreshToken(final String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
