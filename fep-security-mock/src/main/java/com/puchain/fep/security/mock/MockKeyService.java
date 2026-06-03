package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.KeyService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Mock key service for development. Performs Base64 decode as a stand-in for SM2 decryption.
 *
 * <p>Frontend can simulate encryption with {@code btoa(password)} during dev.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Profile("dev")
public class MockKeyService implements KeyService {

    /** Mock public key constant for dev environment only. */
    public static final String MOCK_PUBLIC_KEY = "MOCK_SM2_PUBLIC_KEY_BASE64_FOR_DEV_ONLY";

    /** Mock key ID constant. */
    public static final String MOCK_KEY_ID = "mock-key-v1";

    /**
     * ⚠️ 仅 dev/CI 用，固定 32 字节 mock SM2 私钥（PKCS#8 占位，非真实国密私钥）。
     * 真实实现由 ③ 安全工程师在 fep-security-impl 编写（⛔ Mode E）。
     */
    private static final byte[] MOCK_SIGN_PRIVATE_KEY = {
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10,
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
        0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20
    };

    /**
     * ⚠️ 仅 dev/CI 用，固定 16 字节 mock SM4 主密钥（占位，非真实国密 SM4 密钥）。
     * 真实实现由 ③ 安全工程师在 fep-security-impl 编写（⛔ Mode E），密钥来源 HSM /
     * sealed key store / envelope-encrypted 配置文件。回调凭证加密用。
     * Callback Phase 2b T1（B5 v0.3 修订：⛔ Mode E ownership，AI 起草作 reviewer aid）。
     */
    private static final byte[] MOCK_SM4_CREDENTIAL_MASTER_KEY = {
        0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F
    };

    @Override
    public String getSm2PublicKeyBase64() {
        return MOCK_PUBLIC_KEY;
    }

    @Override
    public String getKeyId() {
        return MOCK_KEY_ID;
    }

    @Override
    public String decryptLoginPassword(final String encryptedBase64, final String keyId) {
        return new String(Base64.getDecoder().decode(encryptedBase64), UTF_8);
    }

    @Override
    public byte[] getSignPrivateKey() {
        // 防御性 clone 避免外部修改污染 mock 常量；⚠️ 仅 dev/CI 用
        return MOCK_SIGN_PRIVATE_KEY.clone();
    }

    @Override
    public byte[] getSm4CredentialMasterKey() {
        // 防御性 clone 避免外部修改污染 mock 常量；⚠️ 仅 dev/CI 用
        return MOCK_SM4_CREDENTIAL_MASTER_KEY.clone();
    }
}
