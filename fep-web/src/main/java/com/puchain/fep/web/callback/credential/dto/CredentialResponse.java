package com.puchain.fep.web.callback.credential.dto;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;

import java.time.LocalDateTime;

/**
 * 回调凭证查询响应（安全：密文 / 明文绝不回显）。
 *
 * <p>仅暴露非敏感元数据 + 布尔配置标记（{@code tokenConfigured} /
 * {@code oauthClientIdConfigured} / {@code oauthClientSecretConfigured}），
 * 让前端判断"是否已配置"而无需取回密文。任何 {@code *Ciphertext} 字节或解密明文
 * 均不出现在本 DTO，从源头杜绝密钥回传 leak。
 * 参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CredentialResponse {

    /** 凭证唯一标识。 */
    private String credentialId;

    /** 关联输出接口 ID。 */
    private String interfaceId;

    /** 鉴权类型。 */
    private InterfaceAuthType authType;

    /** TOKEN HTTP header 名（非敏感元数据）。 */
    private String tokenHeader;

    /** OAUTH2 token 端点 URL（非敏感元数据）。 */
    private String oauthTokenEndpoint;

    /** OAUTH2 scope（非敏感元数据）。 */
    private String oauthScope;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 最近一次密文轮换时间（首次创建为 null）。 */
    private LocalDateTime rotatedAt;

    /** TOKEN 密文是否已配置（mask 标记，不回显密文本身）。 */
    private boolean tokenConfigured;

    /** OAUTH2 client_id 密文是否已配置（mask 标记）。 */
    private boolean oauthClientIdConfigured;

    /** OAUTH2 client_secret 密文是否已配置（mask 标记）。 */
    private boolean oauthClientSecretConfigured;

    /**
     * 从实体构造响应 DTO（剔除全部密文，仅保留元数据 + 配置标记）。
     *
     * @param e 凭证实体（非 null）
     * @return 不含任何密文的响应 DTO
     */
    public static CredentialResponse from(final CallbackCredentialEntity e) {
        final CredentialResponse r = new CredentialResponse();
        r.credentialId = e.getCredentialId();
        r.interfaceId = e.getInterfaceId();
        r.authType = e.getAuthType();
        r.tokenHeader = e.getTokenHeader();
        r.oauthTokenEndpoint = e.getOauthTokenEndpoint();
        r.oauthScope = e.getOauthScope();
        r.createTime = e.getCreateTime();
        r.updateTime = e.getUpdateTime();
        r.rotatedAt = e.getRotatedAt();
        r.tokenConfigured = e.getTokenCiphertext() != null;
        r.oauthClientIdConfigured = e.getOauthClientIdCiphertext() != null;
        r.oauthClientSecretConfigured = e.getOauthClientSecretCiphertext() != null;
        return r;
    }

    /**
     * 获取凭证唯一标识。
     *
     * @return 凭证 ID
     */
    public String getCredentialId() {
        return credentialId;
    }

    /**
     * 设置凭证唯一标识。
     *
     * @param credentialId 凭证 ID
     */
    public void setCredentialId(final String credentialId) {
        this.credentialId = credentialId;
    }

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

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
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
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取最近一次密文轮换时间。
     *
     * @return 轮换时间（首次创建为 null）
     */
    public LocalDateTime getRotatedAt() {
        return rotatedAt;
    }

    /**
     * 设置最近一次密文轮换时间。
     *
     * @param rotatedAt 轮换时间
     */
    public void setRotatedAt(final LocalDateTime rotatedAt) {
        this.rotatedAt = rotatedAt;
    }

    /**
     * TOKEN 密文是否已配置。
     *
     * @return true 表示已配置 token 密文
     */
    public boolean isTokenConfigured() {
        return tokenConfigured;
    }

    /**
     * 设置 TOKEN 密文配置标记。
     *
     * @param tokenConfigured 是否已配置
     */
    public void setTokenConfigured(final boolean tokenConfigured) {
        this.tokenConfigured = tokenConfigured;
    }

    /**
     * OAUTH2 client_id 密文是否已配置。
     *
     * @return true 表示已配置 client_id 密文
     */
    public boolean isOauthClientIdConfigured() {
        return oauthClientIdConfigured;
    }

    /**
     * 设置 OAUTH2 client_id 密文配置标记。
     *
     * @param oauthClientIdConfigured 是否已配置
     */
    public void setOauthClientIdConfigured(final boolean oauthClientIdConfigured) {
        this.oauthClientIdConfigured = oauthClientIdConfigured;
    }

    /**
     * OAUTH2 client_secret 密文是否已配置。
     *
     * @return true 表示已配置 client_secret 密文
     */
    public boolean isOauthClientSecretConfigured() {
        return oauthClientSecretConfigured;
    }

    /**
     * 设置 OAUTH2 client_secret 密文配置标记。
     *
     * @param oauthClientSecretConfigured 是否已配置
     */
    public void setOauthClientSecretConfigured(final boolean oauthClientSecretConfigured) {
        this.oauthClientSecretConfigured = oauthClientSecretConfigured;
    }
}
