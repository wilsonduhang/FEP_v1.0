package com.puchain.fep.web.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性。
 *
 * <p>由 application.yml 的 {@code fep.jwt.*} 注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.jwt")
public class JwtProperties {

    /** HS256 签名密钥（base64 编码，至少 256 bit） */
    private String secret;

    /** 默认 Access Token 有效期秒数: 2 小时 */
    private static final long DEFAULT_ACCESS_TTL = 7200L;

    /** 默认 Refresh Token 有效期秒数: 7 天 */
    private static final long DEFAULT_REFRESH_TTL = 604800L;

    /** Access Token 有效期（秒），默认 7200 = 2 小时 */
    private long accessTokenTtlSeconds = DEFAULT_ACCESS_TTL;

    /** Refresh Token 有效期（秒），默认 604800 = 7 天 */
    private long refreshTokenTtlSeconds = DEFAULT_REFRESH_TTL;

    /** 签发者 */
    private String issuer = "fep";

    /**
     * 获取签名密钥。
     *
     * @return base64 编码的 HS256 密钥
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置签名密钥。
     *
     * @param secret base64 编码的 HS256 密钥
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * 获取 Access Token 有效期。
     *
     * @return 秒数
     */
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    /**
     * 设置 Access Token 有效期。
     *
     * @param v 秒数
     */
    public void setAccessTokenTtlSeconds(final long v) {
        this.accessTokenTtlSeconds = v;
    }

    /**
     * 获取 Refresh Token 有效期。
     *
     * @return 秒数
     */
    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    /**
     * 设置 Refresh Token 有效期。
     *
     * @param v 秒数
     */
    public void setRefreshTokenTtlSeconds(final long v) {
        this.refreshTokenTtlSeconds = v;
    }

    /**
     * 获取签发者。
     *
     * @return 签发者标识
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置签发者。
     *
     * @param issuer 签发者标识
     */
    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }
}
