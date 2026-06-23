package com.puchain.fep.web.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** {@link AggregateRows} 单测：聚合单元 null→0 / Number→long 安全转换。 */
class AggregateRowsTest {

    @Test
    void nullCell_returnsZero() {
        assertThat(AggregateRows.toLong(null)).isZero();
    }

    @Test
    void longCell_returnsValue() {
        assertThat(AggregateRows.toLong(42L)).isEqualTo(42L);
    }

    @Test
    void integerCell_returnsValue() {
        assertThat(AggregateRows.toLong(Integer.valueOf(7))).isEqualTo(7L);
    }

    @Test
    void bigIntegerCell_returnsValue() {
        // JPA SUM 在部分方言下返回 BigInteger/BigDecimal，须经 Number.longValue 归一
        assertThat(AggregateRows.toLong(BigInteger.valueOf(100))).isEqualTo(100L);
    }
}
