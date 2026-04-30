package com.puchain.fep.collector.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotencyKeyGenerator} 单元测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>输出格式：32 位小写十六进制（截断 SHA-256 至 128 bit）</li>
 *   <li>确定性：相同输入相同输出</li>
 *   <li>区分性：不同 adapterId / 不同 sourceRef 输出不同</li>
 *   <li>length-prefix 防 colon 碰撞（核心安全断言）：
 *       {@code ("A", ":B")} 与 {@code ("A:", "B")} 朴素 colon-join 后串相同，
 *       length-prefix 编码后必须不同</li>
 *   <li>参数校验：null adapterId / 空 adapterId / null sourceRef 抛 IllegalArgumentException</li>
 *   <li>边界：空 sourceRef 允许（部分适配器无自然主键）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class IdempotencyKeyGeneratorTest {

    @Test
    void shouldProduce32HexChars() {
        String key = IdempotencyKeyGenerator.generate("ADP1", "row#42");

        assertThat(key)
                .as("输出长度必须为 32 位（截断 SHA-256 至 128 bit）")
                .hasSize(32)
                .as("字符集必须为小写十六进制 [0-9a-f]")
                .matches("[0-9a-f]{32}");
    }

    @Test
    void shouldBeDeterministic() {
        String first = IdempotencyKeyGenerator.generate("ADP1", "row#42");
        String second = IdempotencyKeyGenerator.generate("ADP1", "row#42");

        assertThat(first)
                .as("相同输入必须产生相同的幂等键（哈希函数确定性）")
                .isEqualTo(second);
    }

    @Test
    void shouldDifferOnAdapterIdChange() {
        String keyA = IdempotencyKeyGenerator.generate("ADP_A", "row#42");
        String keyB = IdempotencyKeyGenerator.generate("ADP_B", "row#42");

        assertThat(keyA)
                .as("不同 adapterId 必须产生不同的幂等键")
                .isNotEqualTo(keyB);
    }

    @Test
    void shouldDifferOnSourceRefChange() {
        String keyA = IdempotencyKeyGenerator.generate("ADP1", "row#42");
        String keyB = IdempotencyKeyGenerator.generate("ADP1", "row#43");

        assertThat(keyA)
                .as("不同 sourceRef 必须产生不同的幂等键")
                .isNotEqualTo(keyB);
    }

    /**
     * 核心安全断言：length-prefix 编码必须防 colon 碰撞。
     *
     * <p>朴素拼接 {@code adapterId + ":" + sourceRef} 下：
     * <ul>
     *   <li>{@code ("A", ":B")} → {@code "A::B"}</li>
     *   <li>{@code ("A:", "B")} → {@code "A::B"}</li>
     * </ul>
     * 两者哈希相同，导致幂等键碰撞。length-prefix 编码
     * {@code adapterId.length + ":" + adapterId + sourceRef} 必须区分二者。
     */
    @Test
    void lengthPrefixPreventsColonCollision() {
        String collidingPair1 = IdempotencyKeyGenerator.generate("A", ":B");
        String collidingPair2 = IdempotencyKeyGenerator.generate("A:", "B");

        assertThat(collidingPair1)
                .as("length-prefix 必须防 colon 碰撞：('A',':B') 与 ('A:','B') 在朴素 colon-join 下会相同")
                .isNotEqualTo(collidingPair2);
    }

    @Test
    void shouldRejectNullAdapterId() {
        assertThatThrownBy(() -> IdempotencyKeyGenerator.generate(null, "row#42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adapterId");
    }

    @Test
    void shouldRejectEmptyAdapterId() {
        assertThatThrownBy(() -> IdempotencyKeyGenerator.generate("", "row#42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adapterId");
    }

    @Test
    void shouldRejectNullSourceRef() {
        assertThatThrownBy(() -> IdempotencyKeyGenerator.generate("ADP1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceRef");
    }

    /**
     * 边界：空 sourceRef 必须允许（部分适配器无自然主键，例如全表扫描快照）。
     */
    @Test
    void shouldAllowEmptySourceRef() {
        String key = IdempotencyKeyGenerator.generate("ADP1", "");

        assertThat(key)
                .as("空 sourceRef 必须允许（无自然主键的全量快照场景）")
                .hasSize(32)
                .matches("[0-9a-f]{32}");
    }
}
