package com.puchain.fep.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PasswordComplexityValidator 单元测试（三类中的两类规则）。
 */
class PasswordComplexityValidatorTest {

    private final PasswordComplexityValidator validator = new PasswordComplexityValidator();

    @Test
    void upperAndDigitShouldPass() {
        assertTrue(validator.isValid("ABC12345", null));
    }

    @Test
    void lowerAndDigitShouldPass() {
        assertTrue(validator.isValid("abc12345", null));
    }

    @Test
    void upperAndLowerShouldPass() {
        assertTrue(validator.isValid("AbcDefGh", null));
    }

    @Test
    void allThreeCategoriesShouldPass() {
        assertTrue(validator.isValid("Admin2026", null));
    }

    @Test
    void onlyDigitsShouldFail() {
        assertFalse(validator.isValid("12345678", null));
    }

    @Test
    void onlyLowercaseShouldFail() {
        assertFalse(validator.isValid("abcdefgh", null));
    }

    @Test
    void onlyUppercaseShouldFail() {
        assertFalse(validator.isValid("ABCDEFGH", null));
    }

    @Test
    void specialCharsAloneShouldFail() {
        assertFalse(validator.isValid("!@#$%^&*", null));
    }

    @Test
    void nullShouldPass() {
        // null/空由 @NotBlank 处理
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void emptyShouldPass() {
        assertTrue(validator.isValid("", null));
    }

    @Test
    void specialCharsWithUpperAndLowerShouldPass() {
        assertTrue(validator.isValid("Abc!@#$%", null));
    }
}
