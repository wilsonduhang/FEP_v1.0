package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TriggerOperator} — builds a {@link Predicate} over a trigger
 * field value from a configured operator and value list. The predicate returning
 * {@code true} means the {@code CONDITIONAL_REQUIRED} target field becomes mandatory.
 */
class TriggerOperatorTest {

    @Test
    void equals_trueWhenValueMatchesSingleConfiguredValue() {
        Predicate<String> p = TriggerOperator.EQUALS.toPredicate(List.of("0000"));
        assertThat(p.test("0000")).isTrue();
        assertThat(p.test("9999")).isFalse();
    }

    @Test
    void notEquals_trueWhenValueDiffersFromSingleConfiguredValue() {
        Predicate<String> p = TriggerOperator.NOT_EQUALS.toPredicate(List.of("0000"));
        assertThat(p.test("9999")).isTrue();
        assertThat(p.test("0000")).isFalse();
    }

    @Test
    void in_trueWhenValueInConfiguredSet() {
        Predicate<String> p = TriggerOperator.IN.toPredicate(List.of("0", "00"));
        assertThat(p.test("00")).isTrue();
        assertThat(p.test("99")).isFalse();
    }

    @Test
    void notIn_trueWhenValueNotInConfiguredSet() {
        Predicate<String> p = TriggerOperator.NOT_IN.toPredicate(List.of("0", "00"));
        assertThat(p.test("99")).isTrue();
        assertThat(p.test("0")).isFalse();
    }

    @Test
    void equalsAndNotEquals_requireExactlyOneValue() {
        assertThatThrownBy(() -> TriggerOperator.EQUALS.toPredicate(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TriggerOperator.EQUALS.toPredicate(List.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TriggerOperator.NOT_EQUALS.toPredicate(List.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TriggerOperator.EQUALS.toPredicate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inAndNotIn_rejectEmptyValues() {
        assertThatThrownBy(() -> TriggerOperator.IN.toPredicate(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TriggerOperator.NOT_IN.toPredicate(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TriggerOperator.IN.toPredicate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toPredicate_defensivelyCopiesValues() {
        List<String> vals = new ArrayList<>();
        vals.add("0");
        Predicate<String> p = TriggerOperator.IN.toPredicate(vals);
        vals.add("99"); // 构造后外部追加不应影响谓词
        assertThat(p.test("99")).isFalse();
    }

    @Test
    void equals_capturesImmutableSnapshotOfSingleValue() {
        // 验收 7（EQUALS 路径）：捕获不可变单值快照，外部改原 List 不影响已构造谓词
        List<String> vals = new ArrayList<>();
        vals.add("0000");
        Predicate<String> p = TriggerOperator.EQUALS.toPredicate(vals);
        vals.set(0, "9999");
        assertThat(p.test("0000")).isTrue();
        assertThat(p.test("9999")).isFalse();
    }
}
