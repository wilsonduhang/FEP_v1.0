package com.puchain.fep.collector.assembler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TransitionSeqGenerator} 单元测试（Plan §T7b §7）。
 */
class TransitionSeqGeneratorTest {

    private static final Pattern EIGHT_DIGITS = Pattern.compile("\\d{8}");

    @Test
    void generate100SequentialCalls_yields100UniqueEightDigitNumerics() {
        final TransitionSeqGenerator gen = new TransitionSeqGenerator();
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            final String s = gen.generate();
            assertThat(s).matches(EIGHT_DIGITS);
            seen.add(s);
        }
        assertThat(seen)
                .as("100 sequential generate() must yield 100 unique 8-digit numerics")
                .hasSize(100);
    }

    @Test
    void firstGeneratedValueShouldBeZeroPaddedOne() {
        final TransitionSeqGenerator gen = new TransitionSeqGenerator();
        assertThat(gen.generate()).isEqualTo("00000001");
    }

    @Test
    void sequentialValuesShouldIncreaseMonotonically() {
        final TransitionSeqGenerator gen = new TransitionSeqGenerator();
        final String first = gen.generate();
        final String second = gen.generate();
        final String third = gen.generate();
        assertThat(Integer.parseInt(second)).isEqualTo(Integer.parseInt(first) + 1);
        assertThat(Integer.parseInt(third)).isEqualTo(Integer.parseInt(second) + 1);
    }

    /**
     * M3: 8-digit numeric contract boundary — last legal value emits "99999999",
     * next call must throw IllegalStateException (fail-fast vs silent 9-digit truncation).
     *
     * <p>Counter is set reflectively to 99_999_998 to avoid bloating the production
     * API with a test-only setter.
     */
    @Test
    void generate_atOverflowBoundary_throwsIllegalStateException() throws Exception {
        final TransitionSeqGenerator gen = new TransitionSeqGenerator();
        final Field counterField = TransitionSeqGenerator.class.getDeclaredField("counter");
        counterField.setAccessible(true);
        ((AtomicInteger) counterField.get(gen)).set(TransitionSeqGenerator.MAX_DAILY_SEQUENCE - 1);

        // Last legal value: incrementAndGet -> 99_999_999, format -> "99999999".
        assertThat(gen.generate()).isEqualTo("99999999");

        // Next call: incrementAndGet -> 100_000_000, exceeds MAX_DAILY_SEQUENCE -> throw.
        assertThatThrownBy(gen::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transition sequence overflow")
                .hasMessageContaining("restart required");
    }
}
