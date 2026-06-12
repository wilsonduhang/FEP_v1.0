package com.puchain.fep.processor.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidationResult#firstError()} — Simplify REUSE-R3：
 * 统一三条流水线 7 处 {@code errors().isEmpty() ? "unknown" : errors().get(0)} 重复
 * （含 Batch 侧 "unknown error" 文案 drift）。
 */
class ValidationResultTest {

    @Test
    void firstError_shouldReturnFirstErrorWhenPresent() {
        ValidationResult r = ValidationResult.failed(List.of("e1", "e2"));
        assertThat(r.firstError()).isEqualTo("e1");
    }

    @Test
    void firstError_shouldReturnUnknownWhenEmpty() {
        assertThat(ValidationResult.ok().firstError()).isEqualTo("unknown");
    }
}
