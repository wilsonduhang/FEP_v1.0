package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * SM4 加解密的 Mock 实现 — 明文透传。
 *
 * <p>仅用于开发和测试环境。加密/解密均直接返回输入数据。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class MockCryptoService implements CryptoService {

    private static final Logger log = LoggerFactory.getLogger(MockCryptoService.class);

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key) {
        if (plaintext == null || key == null) {
            throw new IllegalArgumentException("plaintext and key must not be null");
        }
        if (key.length != 16) {
            throw new IllegalArgumentException("SM4 key must be 16 bytes, got " + key.length);
        }
        log.debug("[MOCK] SM4 encrypt called, plaintext length={}", plaintext.length);
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key) {
        if (ciphertext == null || key == null) {
            throw new IllegalArgumentException("ciphertext and key must not be null");
        }
        if (key.length != 16) {
            throw new IllegalArgumentException("SM4 key must be 16 bytes, got " + key.length);
        }
        log.debug("[MOCK] SM4 decrypt called, ciphertext length={}", ciphertext.length);
        return ciphertext;
    }
}
