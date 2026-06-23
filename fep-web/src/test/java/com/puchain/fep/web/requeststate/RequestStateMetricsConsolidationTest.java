package com.puchain.fep.web.requeststate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.puchain.fep.web.common.metrics.MutableTestClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * DEF-MC-1 合并证明：6 个 gauge 在同一 TTL 窗内共享<strong>单次</strong>聚合查询
 * （mock repository，无 Spring context；用可前进的固定 {@link MutableTestClock} 直接数 DB 往返次数——
 * {@code @SpringBootTest} 数不出往返次数，故此处用 Mockito {@code verify} 兜底 6→1 的硬证明）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RequestStateMetricsConsolidationTest {

    /** 可前进的测试时钟。 */
    private final MutableTestClock clock =
            new MutableTestClock(Instant.parse("2026-06-22T00:00:00Z"));

    private static void readAllGauges(final SimpleMeterRegistry reg) {
        for (final RequestStateLifecycle s : RequestStateLifecycle.values()) {
            reg.find(RequestStateMetrics.GAUGE_COUNT).tag("status", s.name()).gauge().value();
        }
        reg.find(RequestStateMetrics.GAUGE_BLOCKED_COUNT).gauge().value();
    }

    private static double gaugeValue(final SimpleMeterRegistry reg, final String status) {
        return reg.find(RequestStateMetrics.GAUGE_COUNT).tag("status", status).gauge().value();
    }

    @Test
    void sixGauges_shareSingleAggregateQuery_withinTtlWindow() {
        final RequestStateRepository repo = mock(RequestStateRepository.class);
        when(repo.aggregateLifecycleAndBlockedCounts())
                .thenReturn(Collections.singletonList(new Object[] {1L, 2L, 3L, 4L, 5L, 6L}));
        final RequestStateMetrics metrics =
                new RequestStateMetrics(repo, clock, Duration.ofSeconds(10));
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        readAllGauges(reg);
        readAllGauges(reg);   // 同窗二次读全部 6 个 gauge

        // 6 个 gauge 同窗仅触发 1 次聚合查询，且零 per-status COUNT（6→1 合并的硬证明）
        verify(repo, times(1)).aggregateLifecycleAndBlockedCounts();
        verify(repo, never()).countByLifecycleStatus(any());
        verify(repo, never()).countByCorrelationBlockedTrue();

        // 列序正确映射 [CREATED,SENT,RESULT_RECEIVED,FAILED,STUCK,blocked]
        assertThat(gaugeValue(reg, "CREATED")).isEqualTo(1.0);
        assertThat(gaugeValue(reg, "SENT")).isEqualTo(2.0);
        assertThat(gaugeValue(reg, "RESULT_RECEIVED")).isEqualTo(3.0);
        assertThat(gaugeValue(reg, "FAILED")).isEqualTo(4.0);
        assertThat(gaugeValue(reg, "STUCK")).isEqualTo(5.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_BLOCKED_COUNT).gauge().value())
                .isEqualTo(6.0);
    }

    @Test
    void afterTtl_requeriesAggregateOncePerWindow() {
        final RequestStateRepository repo = mock(RequestStateRepository.class);
        when(repo.aggregateLifecycleAndBlockedCounts())
                .thenReturn(Collections.singletonList(new Object[] {1L, 0L, 0L, 0L, 0L, 0L}));
        final RequestStateMetrics metrics =
                new RequestStateMetrics(repo, clock, Duration.ofSeconds(10));
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        readAllGauges(reg);                                  // 窗 1 → 1 次
        clock.advance(Duration.ofSeconds(11));               // 越 TTL
        readAllGauges(reg);                                  // 窗 2 → 再 1 次

        verify(repo, times(2)).aggregateLifecycleAndBlockedCounts();
    }

    @Test
    void emptyTable_nullRow_yieldsZeros() {
        final RequestStateRepository repo = mock(RequestStateRepository.class);
        when(repo.aggregateLifecycleAndBlockedCounts())
                .thenReturn(Collections.singletonList(
                        new Object[] {null, null, null, null, null, null}));
        final RequestStateMetrics metrics =
                new RequestStateMetrics(repo, clock, Duration.ofSeconds(10));
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        for (final RequestStateLifecycle s : RequestStateLifecycle.values()) {
            assertThat(gaugeValue(reg, s.name())).isEqualTo(0.0);
        }
        assertThat(reg.find(RequestStateMetrics.GAUGE_BLOCKED_COUNT).gauge().value())
                .isEqualTo(0.0);
    }
}
