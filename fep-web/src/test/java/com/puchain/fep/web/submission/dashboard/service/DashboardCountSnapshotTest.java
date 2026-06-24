package com.puchain.fep.web.submission.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/** {@link InterfaceCountsSnapshot} / {@link PushStatusCountsSnapshot} 列映射 + null→0 单测。 */
class DashboardCountSnapshotTest {

    @Test
    void interfaceCounts_mapsColumnsInOrder() {
        final InterfaceCountsSnapshot s =
                InterfaceCountsSnapshot.fromAggregateRow(new Object[] {10L, 7L});
        assertThat(s.total()).isEqualTo(10L);
        assertThat(s.enabled()).isEqualTo(7L);
    }

    @Test
    void interfaceCounts_nullEnabled_returnsZero() {
        final InterfaceCountsSnapshot s =
                InterfaceCountsSnapshot.fromAggregateRow(new Object[] {0L, null});
        assertThat(s.enabled()).isZero();
    }

    @Test
    void pushStatusCounts_mapsColumnsInOrder() {
        final PushStatusCountsSnapshot s =
                PushStatusCountsSnapshot.fromAggregateRow(new Object[] {500L, 350L, 150L});
        assertThat(s.total()).isEqualTo(500L);
        assertThat(s.pushed()).isEqualTo(350L);
        assertThat(s.pending()).isEqualTo(150L);
    }

    @Test
    void pushStatusCounts_bigIntegerCells_normalizeViaNumber() {
        // JPA SUM 在部分方言下返回 BigInteger，须经 Number.longValue 归一
        final PushStatusCountsSnapshot s = PushStatusCountsSnapshot.fromAggregateRow(
                new Object[] {BigInteger.valueOf(9), BigInteger.valueOf(4), null});
        assertThat(s.total()).isEqualTo(9L);
        assertThat(s.pushed()).isEqualTo(4L);
        assertThat(s.pending()).isZero();
    }
}
