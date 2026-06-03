package com.puchain.fep.web.callback.credential.domain;

import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 接口模式回调凭证实体（SM4 密文列存储 + key_id 支持轮换）。
 *
 * <p>1:1 关联 {@code t_sub_output_interface}，按 {@link InterfaceAuthType} 区分：</p>
 * <ul>
 *   <li>{@link InterfaceAuthType#TOKEN}：使用 {@code tokenCiphertext} + {@code tokenHeader}。</li>
 *   <li>{@link InterfaceAuthType#OAUTH2}：使用 {@code oauthClientIdCiphertext} +
 *       {@code oauthClientSecretCiphertext} + {@code oauthTokenEndpoint} + {@code oauthScope}。</li>
 * </ul>
 *
 * <p>所有字段通过静态工厂 {@link #newToken} / {@link #newOauth} 构造，密文与 keyId
 * 仅经 {@link #rotate} 方法变更，禁止 public setter 旁路状态机。</p>
 *
 * <p>参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "callback_credential",
        uniqueConstraints = @UniqueConstraint(name = "uk_callback_credential_interface",
                columnNames = "interface_id"))
public class CallbackCredentialEntity {

    /** 凭证唯一标识（UUID 32 位无连字符）。 */
    @Id
    @Column(name = "credential_id", length = 32)
    private String credentialId;

    /** 关联输出接口 ID（1:1，{@code t_sub_output_interface.interface_id}）。 */
    @Column(name = "interface_id", nullable = false, length = 32, unique = true)
    private String interfaceId;

    /** 鉴权类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private InterfaceAuthType authType;

    /** TOKEN 鉴权：token 密文（SM4-ECB）。 */
    @Column(name = "token_ciphertext")
    private byte[] tokenCiphertext;

    /** TOKEN 鉴权：HTTP header 名（默认 {@code Authorization}）。 */
    @Column(name = "token_header", length = 50)
    private String tokenHeader;

    /** OAUTH2 鉴权：client_id 密文。 */
    @Column(name = "oauth_client_id_ciphertext")
    private byte[] oauthClientIdCiphertext;

    /** OAUTH2 鉴权：client_secret 密文。 */
    @Column(name = "oauth_client_secret_ciphertext")
    private byte[] oauthClientSecretCiphertext;

    /** OAUTH2 鉴权：token 端点 URL。 */
    @Column(name = "oauth_token_endpoint", length = 500)
    private String oauthTokenEndpoint;

    /** OAUTH2 鉴权：scope。 */
    @Column(name = "oauth_scope", length = 200)
    private String oauthScope;

    /** SM4 主密钥版本号（用于轮换识别）。 */
    @Column(name = "key_id", nullable = false, length = 32)
    private String keyId;

    /** 创建时间。 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 最近一次密文轮换时间（首次创建为 null）。 */
    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    /**
     * JPA 要求的无参构造方法。
     */
    protected CallbackCredentialEntity() {
        /* for JPA */
    }

    /**
     * 构造 TOKEN 鉴权凭证。
     *
     * @param interfaceId 关联输出接口 ID（非 null）
     * @param tokenCipher token 密文（非 null）
     * @param tokenHeader HTTP header 名（null 时默认 {@code Authorization}）
     * @param keyId       SM4 主密钥版本号（非 null）
     * @return 新建 TOKEN 凭证实体
     */
    public static CallbackCredentialEntity newToken(final String interfaceId, final byte[] tokenCipher,
                                                    final String tokenHeader, final String keyId) {
        final CallbackCredentialEntity e = new CallbackCredentialEntity();
        e.credentialId = UUID.randomUUID().toString().replace("-", "");
        e.interfaceId = Objects.requireNonNull(interfaceId, "interfaceId");
        e.authType = InterfaceAuthType.TOKEN;
        // 防御性 clone：避免外部修改污染 entity 内部密文引用（EI_EXPOSE_REP2）
        e.tokenCiphertext = Objects.requireNonNull(tokenCipher, "tokenCipher").clone();
        e.tokenHeader = tokenHeader != null ? tokenHeader : "Authorization";
        e.keyId = Objects.requireNonNull(keyId, "keyId");
        e.createTime = LocalDateTime.now();
        e.updateTime = e.createTime;
        return e;
    }

    /**
     * 构造 OAUTH2 鉴权凭证。
     *
     * @param interfaceId        关联输出接口 ID（非 null）
     * @param clientIdCipher     client_id 密文（非 null）
     * @param clientSecretCipher client_secret 密文（非 null）
     * @param tokenEndpoint      token 端点 URL（非 null）
     * @param scope              OAuth2 scope（可 null）
     * @param keyId              SM4 主密钥版本号（非 null）
     * @return 新建 OAUTH2 凭证实体
     */
    public static CallbackCredentialEntity newOauth(final String interfaceId,
            final byte[] clientIdCipher, final byte[] clientSecretCipher,
            final String tokenEndpoint, final String scope, final String keyId) {
        final CallbackCredentialEntity e = new CallbackCredentialEntity();
        e.credentialId = UUID.randomUUID().toString().replace("-", "");
        e.interfaceId = Objects.requireNonNull(interfaceId, "interfaceId");
        e.authType = InterfaceAuthType.OAUTH2;
        // 防御性 clone：避免外部修改污染 entity 内部密文引用（EI_EXPOSE_REP2）
        e.oauthClientIdCiphertext = Objects.requireNonNull(clientIdCipher, "clientIdCipher").clone();
        e.oauthClientSecretCiphertext =
                Objects.requireNonNull(clientSecretCipher, "clientSecretCipher").clone();
        e.oauthTokenEndpoint = Objects.requireNonNull(tokenEndpoint, "tokenEndpoint");
        e.oauthScope = scope;
        e.keyId = Objects.requireNonNull(keyId, "keyId");
        e.createTime = LocalDateTime.now();
        e.updateTime = e.createTime;
        return e;
    }

    /**
     * 轮换密文与密钥版本。按 {@link #authType} 仅更新对应类型字段，其他字段保持不变。
     *
     * <p>语义：</p>
     * <ul>
     *   <li>TOKEN：仅当 {@code newTokenCipher} 非 null 时更新 {@code tokenCiphertext}。</li>
     *   <li>OAUTH2：仅当 {@code newClientIdCipher} / {@code newClientSecretCipher} 非 null 时分别更新。</li>
     * </ul>
     *
     * @param newTokenCipher        新 token 密文（TOKEN 类型用，可 null 表示不变）
     * @param newClientIdCipher     新 client_id 密文（OAUTH2 类型用，可 null 表示不变）
     * @param newClientSecretCipher 新 client_secret 密文（OAUTH2 类型用，可 null 表示不变）
     * @param newKeyId              新 SM4 主密钥版本号（非 null）
     */
    public void rotate(final byte[] newTokenCipher, final byte[] newClientIdCipher,
                       final byte[] newClientSecretCipher, final String newKeyId) {
        Objects.requireNonNull(newKeyId, "newKeyId");
        if (this.authType == InterfaceAuthType.TOKEN && newTokenCipher != null) {
            this.tokenCiphertext = newTokenCipher.clone();
        }
        if (this.authType == InterfaceAuthType.OAUTH2) {
            if (newClientIdCipher != null) {
                this.oauthClientIdCiphertext = newClientIdCipher.clone();
            }
            if (newClientSecretCipher != null) {
                this.oauthClientSecretCiphertext = newClientSecretCipher.clone();
            }
        }
        this.keyId = newKeyId;
        this.rotatedAt = LocalDateTime.now();
        this.updateTime = this.rotatedAt;
    }

    /**
     * 局部更新非密文元数据字段（partial update）。仅当对应入参非 null 时更新，
     * 与 {@link #rotate} 的密文/keyId 轮换分离。任一字段被更新即刷新 {@code updateTime}。
     *
     * <p>遵循本实体"named transition method only，禁 public setter 旁路状态机"设计：
     * 密文字段经 {@link #rotate}，非密文元数据经本方法。</p>
     *
     * @param newTokenHeader        新 token header 名（null=不变）
     * @param newOauthTokenEndpoint 新 OAUTH2 token 端点 URL（null=不变）
     * @param newOauthScope         新 OAUTH2 scope（null=不变）
     */
    public void updateNonSecretFields(final String newTokenHeader,
                                      final String newOauthTokenEndpoint,
                                      final String newOauthScope) {
        boolean changed = false;
        if (newTokenHeader != null) {
            this.tokenHeader = newTokenHeader;
            changed = true;
        }
        if (newOauthTokenEndpoint != null) {
            this.oauthTokenEndpoint = newOauthTokenEndpoint;
            changed = true;
        }
        if (newOauthScope != null) {
            this.oauthScope = newOauthScope;
            changed = true;
        }
        if (changed) {
            this.updateTime = LocalDateTime.now();
        }
    }

    // ===== Getters =====

    /**
     * 获取凭证唯一标识。
     *
     * @return 凭证 ID（UUID 32 位）
     */
    public String getCredentialId() {
        return credentialId;
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
     * 获取鉴权类型。
     *
     * @return 鉴权类型枚举
     */
    public InterfaceAuthType getAuthType() {
        return authType;
    }

    /**
     * 获取 TOKEN 密文（OAUTH2 类型返回 null）。
     *
     * @return token 密文字节（防御性 copy，TOKEN 类型外为 null）
     */
    public byte[] getTokenCiphertext() {
        return tokenCiphertext == null ? null : tokenCiphertext.clone();
    }

    /**
     * 获取 TOKEN HTTP header 名。
     *
     * @return header 名（默认 {@code Authorization}）
     */
    public String getTokenHeader() {
        return tokenHeader;
    }

    /**
     * 获取 OAUTH2 client_id 密文（TOKEN 类型返回 null）。
     *
     * @return client_id 密文字节（防御性 copy，OAUTH2 类型外为 null）
     */
    public byte[] getOauthClientIdCiphertext() {
        return oauthClientIdCiphertext == null ? null : oauthClientIdCiphertext.clone();
    }

    /**
     * 获取 OAUTH2 client_secret 密文（TOKEN 类型返回 null）。
     *
     * @return client_secret 密文字节（防御性 copy，OAUTH2 类型外为 null）
     */
    public byte[] getOauthClientSecretCiphertext() {
        return oauthClientSecretCiphertext == null ? null : oauthClientSecretCiphertext.clone();
    }

    /**
     * 获取 OAUTH2 token 端点 URL（TOKEN 类型返回 null）。
     *
     * @return token 端点 URL
     */
    public String getOauthTokenEndpoint() {
        return oauthTokenEndpoint;
    }

    /**
     * 获取 OAUTH2 scope（TOKEN 类型返回 null）。
     *
     * @return scope 字符串
     */
    public String getOauthScope() {
        return oauthScope;
    }

    /**
     * 获取 SM4 主密钥版本号。
     *
     * @return key_id
     */
    public String getKeyId() {
        return keyId;
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
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 获取最近一次密文轮换时间。
     *
     * @return 轮换时间（首次创建为 null）
     */
    public LocalDateTime getRotatedAt() {
        return rotatedAt;
    }
}
