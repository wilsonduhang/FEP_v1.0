package com.puchain.fep.web.callback.credential.dto;

/**
 * 回调凭证更新请求（partial update）。
 *
 * <p>所有字段均可选：字段为 {@code null} 表示不修改对应列；密文类字段（{@code token} /
 * {@code oauthClientId} / {@code oauthClientSecret}）非空时 server 端重新 SM4 加密并轮换密文，
 * 非密文字段（{@code tokenHeader} / {@code oauthTokenEndpoint} / {@code oauthScope}）非空时直接更新。
 * 参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CredentialUpdateRequest {

    /** TOKEN 鉴权：新 token 明文（null=不变）。 */
    private String token;

    /** TOKEN 鉴权：新 HTTP header 名（null=不变）。 */
    private String tokenHeader;

    /** OAUTH2 鉴权：新 client_id 明文（null=不变）。 */
    private String oauthClientId;

    /** OAUTH2 鉴权：新 client_secret 明文（null=不变）。 */
    private String oauthClientSecret;

    /** OAUTH2 鉴权：新 token 端点 URL（null=不变）。 */
    private String oauthTokenEndpoint;

    /** OAUTH2 鉴权：新 scope（null=不变）。 */
    private String oauthScope;

    /**
     * 获取 token 明文。
     *
     * @return token 明文（null=不变）
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置 token 明文。
     *
     * @param token token 明文
     */
    public void setToken(final String token) {
        this.token = token;
    }

    /**
     * 获取 token header 名。
     *
     * @return header 名（null=不变）
     */
    public String getTokenHeader() {
        return tokenHeader;
    }

    /**
     * 设置 token header 名。
     *
     * @param tokenHeader header 名
     */
    public void setTokenHeader(final String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }

    /**
     * 获取 OAUTH2 client_id 明文。
     *
     * @return client_id 明文（null=不变）
     */
    public String getOauthClientId() {
        return oauthClientId;
    }

    /**
     * 设置 OAUTH2 client_id 明文。
     *
     * @param oauthClientId client_id 明文
     */
    public void setOauthClientId(final String oauthClientId) {
        this.oauthClientId = oauthClientId;
    }

    /**
     * 获取 OAUTH2 client_secret 明文。
     *
     * @return client_secret 明文（null=不变）
     */
    public String getOauthClientSecret() {
        return oauthClientSecret;
    }

    /**
     * 设置 OAUTH2 client_secret 明文。
     *
     * @param oauthClientSecret client_secret 明文
     */
    public void setOauthClientSecret(final String oauthClientSecret) {
        this.oauthClientSecret = oauthClientSecret;
    }

    /**
     * 获取 OAUTH2 token 端点 URL。
     *
     * @return token 端点 URL（null=不变）
     */
    public String getOauthTokenEndpoint() {
        return oauthTokenEndpoint;
    }

    /**
     * 设置 OAUTH2 token 端点 URL。
     *
     * @param oauthTokenEndpoint token 端点 URL
     */
    public void setOauthTokenEndpoint(final String oauthTokenEndpoint) {
        this.oauthTokenEndpoint = oauthTokenEndpoint;
    }

    /**
     * 获取 OAUTH2 scope。
     *
     * @return scope（null=不变）
     */
    public String getOauthScope() {
        return oauthScope;
    }

    /**
     * 设置 OAUTH2 scope。
     *
     * @param oauthScope scope
     */
    public void setOauthScope(final String oauthScope) {
        this.oauthScope = oauthScope;
    }
}
