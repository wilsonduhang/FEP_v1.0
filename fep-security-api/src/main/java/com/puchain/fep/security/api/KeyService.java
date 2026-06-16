package com.puchain.fep.security.api;

/**
 * SM2 key management service.
 *
 * <p>Provides public key distribution for client-side password encryption
 * and server-side decryption during login.</p>
 *
 * <p><strong>Security note:</strong> 🔓 2026-06-07 muzhou 解禁国密域——SM4 凭证密钥
 * （S1）、SM2 登录密钥（S2a）与 SM2 报文签名密钥（S2b，{@link #getSignPrivateKey()}，
 * 形态 C-ev 经 {@code MessageSignPort}）的真实实现由 AI 编写 + 密码学专项 review。
 * 真实密钥材料永不入 repo，部署期注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface KeyService {

    /**
     * Returns the current SM2 login public key in Base64 encoding.
     *
     * <p><strong>Wire format（GM S2a 抉择②）:</strong> Base64 of the 65-byte uncompressed
     * EC point {@code 04‖x‖y} — NOT X.509 SubjectPublicKeyInfo. The front-end
     * ({@code fep-admin-ui sm2-cipher.ts normalizePublicKey}) decodes Base64 to raw-byte
     * hex and feeds it to sm-crypto directly; an ASN.1-wrapped SPKI would corrupt the
     * key material. Mock provider returns a {@code MOCK_}-prefixed sentinel instead.</p>
     *
     * @return Base64-encoded SM2 login public key (raw uncompressed point)
     * @throws IllegalStateException if SM2 login keys are not configured (impl provider)
     */
    String getSm2PublicKeyBase64();

    /**
     * Returns the key ID (version) of the current active SM4 credential master key.
     *
     * <p>Consumed by {@code CallbackCredentialEncryptionFacade}. Distinct from
     * {@link #getSm2LoginKeyId()} — SM4 credential keys and SM2 login keys rotate
     * independently (GM S2a 抉择⑤).</p>
     *
     * @return key identifier string
     */
    String getKeyId();

    /**
     * Returns the key ID (version) of the current active SM2 login key pair.
     *
     * <p>Distributed alongside {@link #getSm2PublicKeyBase64()} by the public-key
     * endpoint; echoed back in the login request for {@link #decryptLoginPassword}
     * key-version routing. Distinct from {@link #getKeyId()} (SM4 credential key).</p>
     *
     * @return SM2 login key identifier string
     * @throws IllegalStateException if SM2 login keys are not configured (impl provider)
     */
    String getSm2LoginKeyId();

    /**
     * Decrypts a login password that was encrypted with the SM2 login public key.
     *
     * <p><strong>Wire format per provider（GM S2a 抉择③）:</strong> impl provider expects
     * the sm-crypto C1C3C2 hex ciphertext WITHOUT the leading {@code 04} point prefix
     * （fep-admin-ui {@code sm2-cipher.ts doEncrypt(msg, key, 1)} contract）; mock provider
     * expects Base64(plaintext). The parameter is therefore provider-format-specific,
     * not always Base64.</p>
     *
     * @param encryptedPassword SM2 ciphertext in provider wire format (see above)
     * @param keyId             key version used for encryption (for key rotation support)
     * @return cleartext password
     * @throws IllegalArgumentException if the ciphertext is malformed, the keyId is
     *         unknown, or decryption fails
     * @throws IllegalStateException if SM2 login keys are not configured (impl provider)
     */
    String decryptLoginPassword(String encryptedPassword, String keyId);

    /**
     * Returns the current active SM2 private key used for outbound message signing
     * (32-byte scalar {@code d} raw bytes — same wire form as {@link #getAuditSignPrivateKey()},
     * NOT PKCS#8).
     *
     * <p><strong>GM S2b（形态 C-ev，ADR 2026-06-12）:</strong> consumed via
     * {@code MessageSignPort} (the form-agnostic seam), which the converter protocol layer
     * uses to compute the SM3withSM2 signature embedded as an XML comment immediately
     * <em>after</em> {@code </CFX>} (PRD §3.2.1 sample). Under form B (in-process BouncyCastle,
     * current default) the impl reads the configured {@code msg-sign-keys} active scalar;
     * under future form A (external sign-verify server 1818) the private key resides in the
     * external device and this method is not applicable — consumers must depend on
     * {@code MessageSignPort}, not this method.</p>
     *
     * <p>{@code KeyServiceImpl} throws {@link IllegalStateException} when the
     * {@code fep.security.sm2.msg-sign-*} section is not configured. Real key material is
     * injected at deploy time and never enters the repo; dev/CI use GB/T test keys.</p>
     *
     * @return 32-byte SM2 message-sign private key scalar (defensive copy, never {@code null})
     * @throws IllegalStateException if message-sign keys are not configured (impl provider)
     */
    byte[] getSignPrivateKey();

    /**
     * Returns the SM4 master key used for callback credential ciphertext encryption (16 bytes).
     *
     * <p>Consumed by {@code CredentialEncryptionFacade} (Callback Phase 2b T4) to encrypt
     * outbound interface credentials (TOKEN / OAuth2 client_id+secret) at rest in the
     * {@code callback_credential} table. Each call returns the current active SM4
     * master key; rotation is signaled via {@link #getKeyId()} change.</p>
     *
     * <p><strong>🔓 Mode A (2026-06-07 解禁):</strong> muzhou 授权 AI 进入国密域，本方法的
     * 真实实现 {@code KeyServiceImpl}（fep-security-impl）由 AI 编写 + 密码学专项 review。
     * 真实密钥材料仍由密码设备生成、部署期经 HSM/sealed key store/envelope-encrypted 配置
     * 注入，<strong>永不入 repo/git</strong>；dev/CI 用 GB/T 测试密钥。返回 16 字节
     * （GB/T 32907-2016 SM4 密钥长度）防御性副本，调用方不得跨单次加解密保留引用。</p>
     *
     * @return 16-byte SM4 master key (never {@code null})
     */
    byte[] getSm4CredentialMasterKey();

    /**
     * Returns the SM4 master key for a specific key version (16 bytes).
     *
     * <p>Used by {@code CallbackCredentialEncryptionFacade} to decrypt ciphertext that was
     * encrypted under an earlier active key version, enabling multi-version coexistence during
     * key rotation. {@link #getSm4CredentialMasterKey()} (no-arg) returns the current active key
     * used for new encryption; this overload resolves the key recorded on the ciphertext.</p>
     *
     * <p><strong>🔓 Mode A (2026-06-07 解禁):</strong> muzhou 授权 AI 进入国密域，本方法的
     * 真实实现 {@code KeyServiceImpl}（fep-security-impl）由 AI 编写 + 密码学专项 review，从
     * 多版本密钥映射按 keyId 路由。真实密钥材料仍由密码设备生成、部署期经 HSM/sealed key
     * store/envelope-encrypted 配置注入，<strong>永不入 repo/git</strong>；dev/CI 用 GB/T 测试
     * 密钥。返回 16 字节（GB/T 32907-2016）防御性副本，调用方不得跨单次加解密保留引用。</p>
     *
     * @param keyId key version identifier (as recorded on the ciphertext); never {@code null}
     * @return 16-byte SM4 master key for that version (never {@code null})
     * @throws IllegalArgumentException if the key version is unknown/unavailable
     */
    byte[] getSm4CredentialMasterKey(String keyId);

    /**
     * 当前活跃 SM2 审计签名密钥版本号（GM S5 审计 hash 链行签名，落 sign_key_id 列）。
     *
     * <p>与 {@link #getKeyId()}（SM4 凭证）/{@link #getSm2LoginKeyId()}（SM2 登录）
     * 三命名空间独立轮换。</p>
     *
     * @return 审计密钥版本号
     * @throws IllegalStateException impl provider 下审计密钥段未配置
     */
    String getAuditKeyId();

    /**
     * 当前活跃 SM2 审计签名私钥（32 字节标量 d 原始字节，防御性副本）。
     *
     * <p>消费方 = {@code AuditIntegrityService.signEntryHash}（GM S5）。真实密钥
     * 部署期 env 注入永不入 repo；dev/CI 用 GB/T 公开标准测试密钥。</p>
     *
     * @return 32 字节私钥标量（每次新副本）
     * @throws IllegalStateException impl provider 下审计密钥段未配置
     */
    byte[] getAuditSignPrivateKey();

    /**
     * 按版本取 SM2 审计验签公钥（130 hex 未压缩裸点 04∥x∥y）。
     *
     * <p>历史行验签按行记录的 sign_key_id 路由（密钥轮换期多版本共存）。</p>
     *
     * @param keyId 签名时密钥版本，非 null
     * @return 公钥 hex（130 字符）
     * @throws IllegalArgumentException keyId 未知
     * @throws IllegalStateException    impl provider 下审计密钥段未配置
     */
    String getAuditVerifyPublicKeyHex(String keyId);
}
