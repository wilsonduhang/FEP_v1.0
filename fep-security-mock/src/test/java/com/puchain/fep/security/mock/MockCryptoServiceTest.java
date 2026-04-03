package com.puchain.fep.security.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockCryptoService 单元测试。
 */
class MockCryptoServiceTest {

    private MockCryptoService cryptoService;

    private static final byte[] VALID_KEY = "1234567890abcdef".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        cryptoService = new MockCryptoService();
    }

    @Test
    void encryptReturnsPlaintext() {
        byte[] plaintext = "Hello FEP".getBytes(StandardCharsets.UTF_8);
        byte[] result = cryptoService.encrypt(plaintext, VALID_KEY);
        assertArrayEquals(plaintext, result);
    }

    @Test
    void decryptReturnsCiphertext() {
        byte[] ciphertext = "Hello FEP".getBytes(StandardCharsets.UTF_8);
        byte[] result = cryptoService.decrypt(ciphertext, VALID_KEY);
        assertArrayEquals(ciphertext, result);
    }

    @Test
    void encryptRejectsNullPlaintext() {
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(null, VALID_KEY));
    }

    @Test
    void encryptRejectsNullKey() {
        byte[] plaintext = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(plaintext, null));
    }

    @Test
    void encryptRejectsInvalidKeyLength() {
        byte[] plaintext = "test".getBytes(StandardCharsets.UTF_8);
        byte[] badKey = "short".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.encrypt(plaintext, badKey));
    }

    @Test
    void decryptRejectsNullCiphertext() {
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.decrypt(null, VALID_KEY));
    }

    @Test
    void decryptRejectsInvalidKeyLength() {
        byte[] ciphertext = "test".getBytes(StandardCharsets.UTF_8);
        byte[] badKey = "short".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> cryptoService.decrypt(ciphertext, badKey));
    }
}
