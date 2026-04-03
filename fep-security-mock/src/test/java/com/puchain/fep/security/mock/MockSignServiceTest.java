package com.puchain.fep.security.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockSignService 单元测试。
 */
class MockSignServiceTest {

    private MockSignService signService;

    private static final byte[] DUMMY_KEY = "dummy-key-bytes".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        signService = new MockSignService();
    }

    @Test
    void signReturnsMockSignature() {
        byte[] data = "Hello FEP".getBytes(StandardCharsets.UTF_8);
        String signature = signService.sign(data, DUMMY_KEY);
        assertEquals(MockSignService.MOCK_SIGNATURE, signature);
    }

    @Test
    void verifyAlwaysReturnsTrue() {
        byte[] data = "Hello FEP".getBytes(StandardCharsets.UTF_8);
        boolean result = signService.verify(data, "any-signature", DUMMY_KEY);
        assertTrue(result);
    }

    @Test
    void signRejectsNullData() {
        assertThrows(IllegalArgumentException.class,
                () -> signService.sign(null, DUMMY_KEY));
    }

    @Test
    void signRejectsNullKey() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> signService.sign(data, null));
    }

    @Test
    void verifyRejectsNullData() {
        assertThrows(IllegalArgumentException.class,
                () -> signService.verify(null, "sig", DUMMY_KEY));
    }

    @Test
    void verifyRejectsNullSignature() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> signService.verify(data, null, DUMMY_KEY));
    }

    @Test
    void verifyRejectsNullPublicKey() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> signService.verify(data, "sig", null));
    }
}
