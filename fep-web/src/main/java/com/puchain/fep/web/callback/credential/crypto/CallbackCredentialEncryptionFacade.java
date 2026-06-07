package com.puchain.fep.web.callback.credential.crypto;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 凭证加密 facade — callback 模块对 fep-security-api SM4 的统一调用层。
 *
 * <p>明文 String ↔ 密文 byte[] 的双向 roundtrip；密钥版本号 keyId 来源
 * {@link KeyService#getKeyId()} 当前活跃 key。供 {@code CallbackCredentialResolver}
 * 在落库前加密 TOKEN / OAuth2 client_id+secret、读取时还原使用。</p>
 *
 * <p><strong>⛔ Mode A 调用层:</strong> 本 facade 仅 wire 调用 {@link CryptoService} 与
 * {@link KeyService}，<em>不</em>实现任何加密算法或密钥管理逻辑。真实 SM4 算法 + 密钥
 * 材料在 {@code fep-security-impl}（AI 禁入区域，由安全专家人工编写）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackCredentialEncryptionFacade {

    private final CryptoService cryptoService;
    private final KeyService keyService;

    /**
     * 构造 facade，注入安全 API 单例。
     *
     * @param cryptoService SM4 加解密服务，不可为 null
     * @param keyService    密钥/版本服务，不可为 null
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackCredentialEncryptionFacade(final CryptoService cryptoService,
                                      final KeyService keyService) {
        this.cryptoService = Objects.requireNonNull(cryptoService, "cryptoService");
        this.keyService = Objects.requireNonNull(keyService, "keyService");
    }

    /**
     * 加密明文凭证为密文 + 当前 keyId。
     *
     * @param plaintext 明文，非 null 非空
     * @return 加密结果（ciphertext + 当前 keyId）
     * @throws NullPointerException     如果 {@code plaintext} 为 null
     * @throws IllegalArgumentException 如果 {@code plaintext} 为空字符串
     */
    public EncryptedCredential encrypt(final String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        if (plaintext.isEmpty()) {
            throw new IllegalArgumentException("plaintext is empty");
        }
        final byte[] key = keyService.getSm4CredentialMasterKey();
        final byte[] cipher = cryptoService.encrypt(
                plaintext.getBytes(StandardCharsets.UTF_8), key);
        return new EncryptedCredential(cipher, keyService.getKeyId());
    }

    /**
     * 解密密文还原明文。
     *
     * <p>按密文记录的 {@code keyId} 选取对应版本 SM4 master key（{@link
     * KeyService#getSm4CredentialMasterKey(String)}），支持轮换期多版本密钥共存：旧密文用其
     * 加密时的 key 解，活跃 key 轮换后历史密文仍可读。加密恒用当前活跃 key（见 {@link #encrypt}）。</p>
     *
     * @param ciphertext 密文，非 null
     * @param keyId      加密时记录的 keyId（选取对应版本 master key），非 null
     * @return 明文 String（UTF-8 解码）
     * @throws NullPointerException 如果 {@code ciphertext} 或 {@code keyId} 为 null
     */
    public String decrypt(final byte[] ciphertext, final String keyId) {
        Objects.requireNonNull(ciphertext, "ciphertext");
        Objects.requireNonNull(keyId, "keyId");
        final byte[] key = keyService.getSm4CredentialMasterKey(keyId);
        final byte[] plain = cryptoService.decrypt(ciphertext, key);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /**
     * 加密结果记录 — 密文 + 加密时使用的 keyId。
     *
     * <p>{@code ciphertext} 在 canonical constructor 与 accessor 处均做防御性 clone，
     * 避免外部修改污染内部密文引用（SpotBugs EI_EXPOSE_REP）。</p>
     *
     * @param ciphertext 密文字节数组（不可变副本）
     * @param keyId      加密时的密钥版本号
     */
    public record EncryptedCredential(byte[] ciphertext, String keyId) {

        /**
         * Canonical constructor — 对 {@code ciphertext} 做防御性 clone。
         *
         * @param ciphertext 密文字节数组，非 null
         * @param keyId      密钥版本号，非 null
         */
        public EncryptedCredential {
            Objects.requireNonNull(ciphertext, "ciphertext");
            Objects.requireNonNull(keyId, "keyId");
            ciphertext = ciphertext.clone();
        }

        /**
         * 返回密文的防御性副本。
         *
         * @return 密文字节数组副本
         */
        @Override
        public byte[] ciphertext() {
            return ciphertext.clone();
        }
    }
}
