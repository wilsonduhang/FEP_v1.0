package com.puchain.fep.web.config;

import com.puchain.fep.security.api.KeyService;
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
public class TestKeyServiceConfiguration {

    /**
     * Returns a KeyService that performs Base64 decode as SM2 decryption stand-in.
     *
     * @return test KeyService implementation
     */
    @Bean
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
            public String decryptLoginPassword(final String encryptedBase64, final String keyId) {
                return new String(Base64.getDecoder().decode(encryptedBase64), UTF_8);
            }
        };
    }
}
