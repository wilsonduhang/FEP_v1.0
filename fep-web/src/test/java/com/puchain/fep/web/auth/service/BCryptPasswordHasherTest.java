package com.puchain.fep.web.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCryptPasswordHasher 单元测试。
 */
class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashShouldProduceBCryptFormat() {
        String hash = hasher.hash("admin@FEP2026");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"));
        assertEquals(60, hash.length());
    }

    @Test
    void matchesShouldReturnTrueForCorrectPassword() {
        String hash = hasher.hash("MySecret123");
        assertTrue(hasher.matches("MySecret123", hash));
    }

    @Test
    void matchesShouldReturnFalseForWrongPassword() {
        String hash = hasher.hash("MySecret123");
        assertFalse(hasher.matches("WrongPwd", hash));
    }

    @Test
    void hashShouldProduceDifferentValuesEachTime() {
        String hash1 = hasher.hash("same");
        String hash2 = hasher.hash("same");
        assertNotEquals(hash1, hash2);
        assertTrue(hasher.matches("same", hash1));
        assertTrue(hasher.matches("same", hash2));
    }

    @Test
    void hashNullShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }

    @Test
    void hashEmptyShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
    }

    @Test
    void matchesNullShouldReturnFalse() {
        assertFalse(hasher.matches(null, "hash"));
        assertFalse(hasher.matches("pwd", null));
    }
}
