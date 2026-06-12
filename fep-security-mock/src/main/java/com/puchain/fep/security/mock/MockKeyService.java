package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.KeyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class MockKeyService implements KeyService {

    /** Mock public key constant for dev environment only. */
    public static final String MOCK_PUBLIC_KEY = "MOCK_SM2_PUBLIC_KEY_BASE64_FOR_DEV_ONLY";

    /** Mock key ID constant. */
    public static final String MOCK_KEY_ID = "mock-key-v1";

    /**
     * GM S5 mock 审计验签公钥（合法 130-hex 未压缩裸点 = GB/T 32918.5-2017 附录 A
     * 公开标准公钥字面值——v0.3 C-NEW-1：AuditIntegrityServiceImpl.verifyEntry 的
     * parseHex 先于 MockSignService 执行，非 hex 串会致 dev /integrity 恒报假断点；
     * MockSignService 忽略密钥内容，仅需可解析）。
     */
    public static final String MOCK_AUDIT_PUBLIC_KEY_HEX =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    /**
     * ⚠️ 仅 dev/CI 用，固定 32 字节 mock SM2 私钥（PKCS#8 占位，非真实国密私钥）。
     * 真实报文签名私钥属 S2b（🔓 解禁治理，待 §0.3 决策门定调后 AI 实施 + 密码学专项 review）。
     */
    private static final byte[] MOCK_SIGN_PRIVATE_KEY = {
        0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10,
        0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
        0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20
    };

    /**
     * ⚠️ 仅 dev/CI 用，固定 16 字节 mock SM4 主密钥（占位，非真实国密 SM4 密钥）。
     * 真实实现 KeyServiceImpl（🔓 解禁治理，S1 已实装 + 密码学专项 review），密钥来源 HSM /
     * sealed key store / envelope-encrypted 配置文件。回调凭证加密用。
     * Callback Phase 2b T1 起草；🔓 2026-06-07 解禁后 S1 实装真实路径。
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
    public String getSm2LoginKeyId() {
        // GM S2a: mock 域 SM2 登录 keyId 与 SM4 凭证 keyId 共用同一常量
        return MOCK_KEY_ID;
    }

    @Override
    public String getAuditKeyId() {
        // GM S5: 审计 keyId 在 mock 域共用同一常量
        return MOCK_KEY_ID;
    }

    @Override
    public byte[] getAuditSignPrivateKey() {
        // 防御性 clone；MockSignService 忽略内容，⚠️ 仅 dev/CI 用
        return MOCK_SIGN_PRIVATE_KEY.clone();
    }

    @Override
    public String getAuditVerifyPublicKeyHex(final String keyId) {
        // 任意 keyId 返回合法 130-hex（v0.3 C-NEW-1）；MockSignService 验签恒 true
        return MOCK_AUDIT_PUBLIC_KEY_HEX;
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

    @Override
    public byte[] getSm4CredentialMasterKey(final String keyId) {
        if (MOCK_KEY_ID.equals(keyId)) {
            return MOCK_SM4_CREDENTIAL_MASTER_KEY.clone();
        }
        // ⚠️ 仅 dev/CI：按 keyId 确定性派生 16 字节占位 key（非真实国密派生）。
        // 真实多版本密钥路由 KeyServiceImpl（🔓 解禁治理，S1 已实装）。
        return deriveMock16(keyId);
    }

    private static byte[] deriveMock16(final String keyId) {
        try {
            final byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(keyId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final byte[] key = new byte[16];
            System.arraycopy(digest, 0, key, 0, 16);
            return key;
        } catch (final java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
