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
}
