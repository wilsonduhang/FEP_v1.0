package com.puchain.fep.common.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ValueOfEnumValidator} 单元测试。
 */
class ValueOfEnumValidatorTest {

    enum SampleEnum { ALPHA, BETA, GAMMA }

    private ValueOfEnumValidator validator;
    private ConstraintValidatorContext ctx;

    @BeforeEach
    void setUp() {
        validator = new ValueOfEnumValidator();
        ctx = Mockito.mock(ConstraintValidatorContext.class);
        validator.initialize(new ValueOfEnum() {
            @Override public Class<? extends Enum<?>> enumClass() { return SampleEnum.class; }
            @Override public String message() { return ""; }
            @Override public Class<?>[] groups() { return new Class<?>[0]; }
            @Override public Class<? extends jakarta.validation.Payload>[] payload() {
                @SuppressWarnings("unchecked")
                Class<? extends jakarta.validation.Payload>[] empty =
                        (Class<? extends jakarta.validation.Payload>[]) new Class<?>[0];
                return empty;
            }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValueOfEnum.class; }
        });
    }

    @Test
    void validConstantNameShouldPass() {
        assertTrue(validator.isValid("ALPHA", ctx));
        assertTrue(validator.isValid("BETA", ctx));
        assertTrue(validator.isValid("GAMMA", ctx));
    }

    @Test
    void invalidConstantNameShouldFail() {
        assertFalse(validator.isValid("DELTA", ctx));
        assertFalse(validator.isValid("alpha", ctx), "case-sensitive");
    }

    @Test
    void nullShouldPass_deferredToNotBlank() {
        assertTrue(validator.isValid(null, ctx));
    }

    @Test
    void emptyStringShouldPass_deferredToNotBlank() {
        assertTrue(validator.isValid("", ctx));
    }
}
