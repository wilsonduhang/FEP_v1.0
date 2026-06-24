package com.puchain.fep.web.submission.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** {@link TrendDayRow} / {@link DistributionRow} 列映射单测。 */
class DashboardRowSnapshotTest {

    @Test
    void trendDayRow_mapsColumnsInOrder() {
        final TrendDayRow r = TrendDayRow.fromAggregateRow(
                new Object[] {"2026-06-20", 9L, 4L});
        assertThat(r.date()).isEqualTo("2026-06-20");
        assertThat(r.pushed()).isEqualTo(9L);
        assertThat(r.pending()).isEqualTo(4L);
    }

    @Test
    void distributionRow_mapsColumnsInOrder() {
        final DistributionRow r = DistributionRow.fromAggregateRow(
                new Object[] {"3101", 42L});
        assertThat(r.name()).isEqualTo("3101");
        assertThat(r.value()).isEqualTo(42L);
    }

    // santa MINOR-2 加固：显式验证防御层 null→0（JPQL 理论不产 null，但单元验证防御契约）
    @Test
    void trendDayRow_nullCounts_returnZero() {
        final TrendDayRow r = TrendDayRow.fromAggregateRow(
                new Object[] {"2026-06-20", null, null});
        assertThat(r.pushed()).isZero();
        assertThat(r.pending()).isZero();
    }

    @Test
    void distributionRow_nullValue_returnsZero() {
        final DistributionRow r = DistributionRow.fromAggregateRow(
                new Object[] {"3101", null});
        assertThat(r.value()).isZero();
    }
}
