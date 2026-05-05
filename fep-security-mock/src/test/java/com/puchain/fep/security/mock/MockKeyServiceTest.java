package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.KeyService;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockKeyService}.
 */
class MockKeyServiceTest {

    private final KeyService keyService = new MockKeyService();

    @Test
    void getSm2PublicKeyBase64_shouldReturnNonBlankString() {
        assertThat(keyService.getSm2PublicKeyBase64()).isNotBlank();
    }

    @Test
    void getKeyId_shouldReturnMockKeyV1() {
        assertThat(keyService.getKeyId()).isEqualTo("mock-key-v1");
    }

    @Test
    void decryptLoginPassword_shouldBase64Decode() {
        String clearPassword = "Abc12345";
        String encrypted = Base64.getEncoder().encodeToString(clearPassword.getBytes(UTF_8));
        String decrypted = keyService.decryptLoginPassword(encrypted, "mock-key-v1");
        assertThat(decrypted).isEqualTo(clearPassword);
    }

    @Test
    void decryptLoginPassword_shouldHandleUtf8() {
        String clearPassword = "密码Test1";
        String encrypted = Base64.getEncoder().encodeToString(clearPassword.getBytes(UTF_8));
        String decrypted = keyService.decryptLoginPassword(encrypted, "mock-key-v1");
        assertThat(decrypted).isEqualTo(clearPassword);
    }

    @Test
    void getSignPrivateKey_shouldReturn32MockBytes() {
        // P5 T5: pin mock contract — outbound 加签器消费 32 字节 mock 私钥（dev/CI only）
        assertThat(keyService.getSignPrivateKey()).hasSize(32);
    }
}
