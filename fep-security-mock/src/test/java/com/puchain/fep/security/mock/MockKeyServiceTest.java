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

    @Test
    void getSm4CredentialMasterKey_shouldReturn16MockBytes() {
        // Callback Phase 2b T1 (B5 v0.3): pin mock contract — credential 加密器消费 16 字节 SM4 主密钥
        // SM4 key length per GB/T 32907-2016 = 128 bits = 16 bytes
        assertThat(keyService.getSm4CredentialMasterKey()).hasSize(16);
    }

    @Test
    void getSm4CredentialMasterKey_shouldReturnDefensiveCopy() {
        // 防御性 clone 验证：修改返回值不污染 mock 常量
        byte[] firstCall = keyService.getSm4CredentialMasterKey();
        firstCall[0] = (byte) 0xFF;
        byte[] secondCall = keyService.getSm4CredentialMasterKey();
        assertThat(secondCall[0]).isNotEqualTo((byte) 0xFF);
    }
}
