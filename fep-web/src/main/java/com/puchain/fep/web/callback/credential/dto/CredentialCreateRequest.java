package com.puchain.fep.web.callback.credential.dto;

import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 回调凭证新建请求。
 *
 * <p>明文凭证字段（{@code token} / {@code oauthClientId} / {@code oauthClientSecret}）
 * 经 HTTPS POST 提交，server 端 SM4 加密后落库为密文列，绝不持久化明文。
 * 参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CredentialCreateRequest {

    /** 关联输出接口 ID（非空）。 */
    @NotBlank
    private String interfaceId;

    /** 鉴权类型（非空）。 */
    @NotNull
    private InterfaceAuthType authType;

    /** TOKEN 鉴权：token 明文（server 端加密，TOKEN 类型必填）。 */
    private String token;

    /** TOKEN 鉴权：HTTP header 名（可选，默认 {@code Authorization}）。 */
    private String tokenHeader;

    /** OAUTH2 鉴权：client_id 明文（OAUTH2 类型必填）。 */
    private String oauthClientId;

    /** OAUTH2 鉴权：client_secret 明文（OAUTH2 类型必填）。 */
    private String oauthClientSecret;

    /** OAUTH2 鉴权：token 端点 URL（OAUTH2 类型必填）。 */
    private String oauthTokenEndpoint;

    /** OAUTH2 鉴权：scope（可选）。 */
    private String oauthScope;

    /**
     * 获取关联输出接口 ID。
     *
     * @return 接口 ID
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * 设置关联输出接口 ID。
     *
     * @param interfaceId 接口 ID
     */
    public void setInterfaceId(final String interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * 获取鉴权类型。
     *
     * @return 鉴权类型
     */
    public InterfaceAuthType getAuthType() {
        return authType;
    }

    /**
     * 设置鉴权类型。
     *
     * @param authType 鉴权类型
     */
    public void setAuthType(final InterfaceAuthType authType) {
        this.authType = authType;
    }

    /**
     * 获取 token 明文。
     *
     * @return token 明文
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
     * @return header 名
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
     * @return client_id 明文
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
     * @return client_secret 明文
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
     * @return token 端点 URL
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
     * @return scope
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
