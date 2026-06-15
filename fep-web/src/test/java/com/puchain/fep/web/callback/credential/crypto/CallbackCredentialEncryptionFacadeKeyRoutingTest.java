package com.puchain.fep.web.callback.credential.crypto;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CallbackCredentialEncryptionFacade} 跨 keyId 路由正确性（FR-INFRA-CALLBACK-CREDENTIAL）。
 *
 * <p>用 JDK AES-128 <strong>密钥敏感</strong> cipher 替身（错 key→BadPadding）+ 2 版本
 * KeyService 替身，验 facade 按记录 keyId 选取对应版本 master key 还原密文——dev mock
 * 明文透传会让此路由 bug 假绿，本测试以密钥敏感 cipher 揭穿。真 SM4 算法由 fep-security-impl
 * GB/T 向量测试背书，不在此范围。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackCredentialEncryptionFacadeKeyRoutingTest {

    /** AES-128/ECB/PKCS5 — JDK 内置密钥敏感 cipher，作 SM4 的测试替身（仅验路由，非国密）。 */
    private static final class AesCryptoService implements CryptoService {
        @Override
        public byte[] encrypt(final byte[] plaintext, final byte[] key) {
            return run(Cipher.ENCRYPT_MODE, plaintext, key);
        }

        @Override
        public byte[] decrypt(final byte[] ciphertext, final byte[] key) {
            return run(Cipher.DECRYPT_MODE, ciphertext, key);
        }

        private static byte[] run(final int mode, final byte[] data, final byte[] key) {
            try {
                final Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
                c.init(mode, new SecretKeySpec(key, "AES"));
                return c.doFinal(data);
            } catch (final Exception e) {
                throw new IllegalStateException("AES test cipher failure", e);
            }
        }
    }

    /** 2 版本 KeyService 替身：active=v2；v1/v2 各一把不同的 16-byte AES key。 */
    private static final class TwoVersionKeyService implements KeyService {
        private static final byte[] KEY_V1 = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        private static final byte[] KEY_V2 = "FEDCBA9876543210".getBytes(StandardCharsets.UTF_8);

        @Override
        public String getKeyId() {
            return "v2";
        }

        @Override
        public byte[] getSm4CredentialMasterKey() {
            return KEY_V2.clone();
        }

        @Override
        public byte[] getSm4CredentialMasterKey(final String keyId) {
            return "v1".equals(keyId) ? KEY_V1.clone() : KEY_V2.clone();
        }

        // 以下 SPI 本测试不触发，返回占位/抛出即可。
        @Override
        public String getSm2PublicKeyBase64() {
            return "x";
        }

        @Override
        public String getSm2LoginKeyId() {
            return "v2";
        }

        @Override
        public String getAuditKeyId() {
            return "v2";
        }

        @Override
        public byte[] getAuditSignPrivateKey() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAuditVerifyPublicKeyHex(final String keyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String decryptLoginPassword(final String encryptedPassword, final String keyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getSignPrivateKey() {
            throw new UnsupportedOperationException();
        }
    }

    private final CallbackCredentialEncryptionFacade facade =
            new CallbackCredentialEncryptionFacade(new AesCryptoService(), new TwoVersionKeyService());

    @Test
    void encrypt_recordsActiveKeyId_andDecryptsWithCorrectKey() {
        final EncryptedCredential enc = facade.encrypt("secret");

        assertThat(enc.keyId()).isEqualTo("v2");
        assertThat(facade.decrypt(enc.ciphertext(), "v2")).isEqualTo("secret");
    }

    @Test
    void decrypt_withWrongKeyId_failsToRecover() {
        final EncryptedCredential enc = facade.encrypt("secret");

        // 错 keyId → 错 key → AES PKCS5 unpad 失败（密钥敏感证据；mock passthrough 会假绿）
        assertThatThrownBy(() -> facade.decrypt(enc.ciphertext(), "v1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reEncrypt_producesDifferentCiphertext_butSamePlaintext() {
        final EncryptedCredential first = facade.encrypt("secret");
        final String recovered = facade.decrypt(first.ciphertext(), first.keyId());
        final EncryptedCredential second = facade.encrypt(recovered);

        assertThat(second.keyId()).isEqualTo("v2");
        assertThat(facade.decrypt(second.ciphertext(), second.keyId())).isEqualTo("secret");
    }
}
