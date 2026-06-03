package com.puchain.fep.web.callback.credential.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.callback.credential.crypto.CredentialEncryptionFacade.EncryptedCredential;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CredentialEncryptionFacade}.
 *
 * <p>Mocks {@link CryptoService} + {@link KeyService} (the SM4 algorithm and key
 * material live in {@code fep-security-impl}, AI-forbidden). Verifies the facade
 * correctly wires plaintext encryption/decryption to the security API and surfaces
 * the active keyId, plus null/empty input guards and defensive byte[] cloning.</p>
 */
@ExtendWith(MockitoExtension.class)
class CredentialEncryptionFacadeTest {

    @Mock
    private CryptoService cryptoService;

    @Mock
    private KeyService keyService;

    private CredentialEncryptionFacade facade;

    @BeforeEach
    void setUp() {
        facade = new CredentialEncryptionFacade(cryptoService, keyService);
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
        when(keyService.getSm4CredentialMasterKey()).thenReturn(new byte[16]);
        when(cryptoService.decrypt(eq(new byte[] {9, 9, 9}), any()))
                .thenReturn("plain-token".getBytes(StandardCharsets.UTF_8));

        String result = facade.decrypt(new byte[] {9, 9, 9}, "KEY-V1");

        assertThat(result).isEqualTo("plain-token");
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
