package com.puchain.fep.web.config;

import com.puchain.fep.security.api.KeyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Test configuration providing a {@link KeyService} bean for integration tests.
 *
 * <p>The real {@code MockKeyService} from fep-security-mock is
 * {@code @Profile("dev")}, so it does not load in test context.
 * This configuration provides an equivalent bean for test use.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class TestKeyServiceConfiguration {

    /**
     * Returns a KeyService that performs Base64 decode as SM2 decryption stand-in.
     *
     * <p>Only registered when no other {@link KeyService} bean is present in the
     * context. Under the default {@code dev} profile, {@code MockKeyService} from
     * fep-security-mock is active and takes precedence; this bean then steps
     * aside so {@code @MockBean KeyService} injection is unambiguous.</p>
     *
     * @return test KeyService implementation
     */
    @Bean
    @ConditionalOnMissingBean(KeyService.class)
    public KeyService keyService() {
        return new KeyService() {
            @Override
            public String getSm2PublicKeyBase64() {
                return "MOCK_SM2_PUBLIC_KEY_BASE64_FOR_DEV_ONLY";
            }

            @Override
            public String getKeyId() {
                return "mock-key-v1";
            }

            @Override
            public String getSm2LoginKeyId() {
                // GM S2a: test mock 域 SM2 登录 keyId 与 SM4 凭证 keyId 共用
                return "mock-key-v1";
            }

            @Override
            public String decryptLoginPassword(final String encryptedBase64, final String keyId) {
                return new String(Base64.getDecoder().decode(encryptedBase64), UTF_8);
            }

            @Override
            public byte[] getSignPrivateKey() {
                // ⚠️ 仅 test 用 mock 私钥（非真实国密 SM2 私钥）；真实实现 ⛔ Mode E
                return new byte[32];
            }

            @Override
            public byte[] getSm4CredentialMasterKey() {
                // ⚠️ 仅 test 用 mock SM4 主密钥（非真实国密 SM4 密钥）；真实实现 ⛔ Mode E
                // Callback Phase 2b T1 (B5 v0.3): ⛔ Mode E ownership; 16 bytes for SM4-CBC
                return new byte[16];
            }

            @Override
            public byte[] getSm4CredentialMasterKey(final String keyId) {
                // ⚠️ 仅 test：当前活跃版本须与无参版一致（与 MockKeyService MOCK_KEY_ID 分支对称）
                if ("mock-key-v1".equals(keyId)) {
                    return new byte[16];
                }
                // 历史版本按 keyId 确定性派生 16 字节，镜像 MockKeyService；真实实现 ⛔ Mode E
                try {
                    final byte[] d = java.security.MessageDigest.getInstance("SHA-256")
                            .digest(keyId.getBytes(UTF_8));
                    final byte[] k = new byte[16];
                    System.arraycopy(d, 0, k, 0, 16);
                    return k;
                } catch (final java.security.NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
