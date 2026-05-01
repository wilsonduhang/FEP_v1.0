package com.puchain.fep.collector.assembler;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

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
}
