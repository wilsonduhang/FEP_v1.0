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
    void getSm2LoginKeyId_returnsMockKeyId() {
        // GM S2a: SM2 登录 keyId 与 SM4 凭证 keyId 在 mock 域共用同一常量
        assertThat(keyService.getSm2LoginKeyId()).isEqualTo("mock-key-v1");
    }

    @Test
    void getAuditKeyId_returnsMockKeyId() {
        // GM S5: 审计 keyId 在 mock 域共用同一常量
        assertThat(keyService.getAuditKeyId()).isEqualTo("mock-key-v1");
    }

    @Test
    void getAuditVerifyPublicKeyHex_isParseableUncompressedPointHex() {
        // GM S5 v0.3 C-NEW-1：AuditIntegrityServiceImpl.verifyEntry 的 parseHex 先于
        // MockSignService 执行——mock 公钥必须为合法 130-hex，否则 dev /integrity 恒报假断点
        assertThat(keyService.getAuditVerifyPublicKeyHex("any"))
                .matches("04[0-9a-fA-F]{128}");
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

    @Test
    void getSm4CredentialMasterKey_withCurrentKeyId_matchesNoArg() {
        // Phase 2c-B T5: keyId 重载在当前活跃版本上须与无参版一致（向后兼容）
        assertThat(keyService.getSm4CredentialMasterKey(MockKeyService.MOCK_KEY_ID))
                .isEqualTo(keyService.getSm4CredentialMasterKey());
    }

    @Test
    void getSm4CredentialMasterKey_keyedVariant_is16BytesAndDeterministic() {
        // 历史版本 keyId 派生 16 字节 mock key，确定性（同 keyId 同 key）
        byte[] k1 = keyService.getSm4CredentialMasterKey("mock-key-v2");
        byte[] k1b = keyService.getSm4CredentialMasterKey("mock-key-v2");
        assertThat(k1).hasSize(16).isEqualTo(k1b);
    }

    @Test
    void getSm4CredentialMasterKey_differentKeyIds_differentKeys() {
        // 不同 keyId 派生不同 key（dev/CI 多版本解密测试前提）
        assertThat(keyService.getSm4CredentialMasterKey("mock-key-v2"))
                .isNotEqualTo(keyService.getSm4CredentialMasterKey("mock-key-v3"));
    }
}
