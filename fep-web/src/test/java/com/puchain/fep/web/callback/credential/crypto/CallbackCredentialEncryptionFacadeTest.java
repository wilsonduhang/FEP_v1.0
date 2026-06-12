package com.puchain.fep.web.callback.credential.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CallbackCredentialEncryptionFacade}.
 *
 * <p>Mocks {@link CryptoService} + {@link KeyService} (the SM4 algorithm and key
 * material live in {@code fep-security-impl}, AI-forbidden). Verifies the facade
 * correctly wires plaintext encryption/decryption to the security API and surfaces
 * the active keyId, plus null/empty input guards and defensive byte[] cloning.</p>
 */
@ExtendWith(MockitoExtension.class)
class CallbackCredentialEncryptionFacadeTest {

    @Mock
    private CryptoService cryptoService;

    @Mock
    private KeyService keyService;

    private CallbackCredentialEncryptionFacade facade;

    @BeforeEach
    void setUp() {
        facade = new CallbackCredentialEncryptionFacade(cryptoService, keyService);
    }

    @Test
    void encryptCallsCryptoServiceWithMasterKeyAndCurrentKeyId() {
        byte[] key = new byte[16];
        when(keyService.getSm4CredentialMasterKey()).thenReturn(key);
        when(keyService.getKeyId()).thenReturn("KEY-V1");
        when(cryptoService.encrypt(any(), eq(key))).thenReturn(new byte[] {9, 9, 9});

        EncryptedCredential result = facade.encrypt("plain-token");

        assertThat(result.ciphertext()).containsExactly(9, 9, 9);
        assertThat(result.keyId()).isEqualTo("KEY-V1");
    }

    @Test
    void encryptUsesUtf8EncodedPlaintextBytes() {
        byte[] key = new byte[16];
        when(keyService.getSm4CredentialMasterKey()).thenReturn(key);
        when(keyService.getKeyId()).thenReturn("KEY-V1");
        when(cryptoService.encrypt(eq("令牌".getBytes(StandardCharsets.UTF_8)), eq(key)))
                .thenReturn(new byte[] {7});

        EncryptedCredential result = facade.encrypt("令牌");

        assertThat(result.ciphertext()).containsExactly(7);
    }

    @Test
    void decryptCallsCryptoServiceAndReturnsUtf8String() {
        when(keyService.getSm4CredentialMasterKey("KEY-V1")).thenReturn(new byte[16]);
        when(cryptoService.decrypt(eq(new byte[] {9, 9, 9}), any()))
                .thenReturn("plain-token".getBytes(StandardCharsets.UTF_8));

        String result = facade.decrypt(new byte[] {9, 9, 9}, "KEY-V1");

        assertThat(result).isEqualTo("plain-token");
    }

    @Test
    void decryptRoutesToKeyIdSpecificMasterKey() {
        when(keyService.getSm4CredentialMasterKey("k-old")).thenReturn(new byte[16]);
        when(cryptoService.decrypt(any(), any()))
                .thenReturn("plain".getBytes(StandardCharsets.UTF_8));

        facade.decrypt(new byte[] {9}, "k-old");

        verify(keyService).getSm4CredentialMasterKey("k-old");
        verify(keyService, never()).getSm4CredentialMasterKey();
    }

    @Test
    void encryptThenDecryptCrossKeyWithKeySensitiveCipher() {
        // key 敏感可逆 cipher（XOR by key[0]）+ 每 keyId 不同 key 的 KeyService，
        // 证明"旧 key 密文用旧 key 解出 / 用新 key 解不出"（mock 透传无法证明，故用真 key 敏感 cipher）
        final CryptoService xor = new CryptoService() {
            @Override
            public byte[] encrypt(final byte[] p, final byte[] k) {
                return xorAll(p, k[0]);
            }

            @Override
            public byte[] decrypt(final byte[] c, final byte[] k) {
                return xorAll(c, k[0]);
            }
        };
        final KeyService ks = perKeyIdKeyService();
        final CallbackCredentialEncryptionFacade keyedFacade =
                new CallbackCredentialEncryptionFacade(xor, ks);

        final byte[] oldCipher = xor.encrypt("secret".getBytes(StandardCharsets.UTF_8), key16((byte) 0x22));
        assertThat(keyedFacade.decrypt(oldCipher, "k-old")).isEqualTo("secret");
        assertThat(keyedFacade.decrypt(oldCipher, "k-new")).isNotEqualTo("secret");
    }

    private static KeyService perKeyIdKeyService() {
        return new KeyService() {
            @Override
            public String getSm2PublicKeyBase64() {
                return "x";
            }

            @Override
            public String getKeyId() {
                return "k-new";
            }

            @Override
            public String getSm2LoginKeyId() {
                return "k-new";
            }

            @Override
            public String getAuditKeyId() {
                return "k-new";
            }

            @Override
            public byte[] getAuditSignPrivateKey() {
                return new byte[32];
            }

            @Override
            public String getAuditVerifyPublicKeyHex(final String keyId) {
                // 合法 130-hex（GB/T 公开标准公钥字面值，v0.3 C-NEW-1）
                return "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                        + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";
            }

            @Override
            public String decryptLoginPassword(final String e, final String id) {
                return e;
            }

            @Override
            public byte[] getSignPrivateKey() {
                return new byte[32];
            }

            @Override
            public byte[] getSm4CredentialMasterKey() {
                return key16((byte) 0x11);
            }

            @Override
            public byte[] getSm4CredentialMasterKey(final String keyId) {
                return "k-old".equals(keyId) ? key16((byte) 0x22) : key16((byte) 0x11);
            }
        };
    }

    private static byte[] xorAll(final byte[] in, final byte x) {
        final byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) (in[i] ^ x);
        }
        return out;
    }

    private static byte[] key16(final byte b) {
        final byte[] k = new byte[16];
        java.util.Arrays.fill(k, b);
        return k;
    }

    @Test
    void encryptNullPlaintextThrows() {
        assertThatThrownBy(() -> facade.encrypt(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void encryptEmptyPlaintextThrows() {
        assertThatThrownBy(() -> facade.encrypt(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void decryptNullCiphertextThrows() {
        assertThatThrownBy(() -> facade.decrypt(null, "KEY-V1"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void encryptedCredentialDefensivelyClonesCiphertextOnConstruction() {
        byte[] source = {1, 2, 3};
        EncryptedCredential ec = new EncryptedCredential(source, "KEY-V1");

        source[0] = 99;

        assertThat(ec.ciphertext()).containsExactly(1, 2, 3);
    }

    @Test
    void encryptedCredentialDefensivelyClonesCiphertextOnAccess() {
        EncryptedCredential ec = new EncryptedCredential(new byte[] {1, 2, 3}, "KEY-V1");

        byte[] accessed = ec.ciphertext();
        accessed[0] = 99;

        assertThat(ec.ciphertext()).containsExactly(1, 2, 3);
    }
}
