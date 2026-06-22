package com.puchain.fep.web.requeststate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour for {@link RequestStateMetrics}: lifecycle-status gauges + separate blocked gauge.
 *
 * <p>gauge 值在 scrape 时实时回查 DB，故用 {@code @SpringBootTest} 注入真实 repository（镜像
 * {@link RequestStateServiceTest}，无 {@code @Transactional}，物理 {@code deleteAll()} 清理）。
 * {@link RequestStateMetrics#bindTo} 注册到独立 {@link SimpleMeterRegistry} 以隔离断言。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("RequestStateMetrics: per-lifecycle gauges + blocked gauge distinct from STUCK")
class RequestStateMetricsTest {

    @Autowired
    private RequestStateMetrics metrics;

    @Autowired
    private RequestStateService service;

    @Autowired
    private RequestStateRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void gauges_reportPerLifecycleCounts() {
        service.create("00000001", "3101", "QID-1");          // CREATED, not blocked
        service.create("00000002", "3101", "QID-2");
        service.markSent("00000002");                          // SENT, not blocked

        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "CREATED").gauge().value()).isEqualTo(1.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "SENT").gauge().value()).isEqualTo(1.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "RESULT_RECEIVED").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void blockedGauge_isDistinctFromStuckCount() {
        // 3115 is correlation_blocked; it must surface in the blocked gauge, not STUCK.
        service.create("00000003", "3115", "QID-3");
        service.markSent("00000003");

        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        assertThat(reg.find(RequestStateMetrics.GAUGE_BLOCKED_COUNT).gauge().value())
                .isEqualTo(1.0);
        // blocked row sits in SENT, not STUCK
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "STUCK").gauge().value()).isEqualTo(0.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "SENT").gauge().value()).isEqualTo(1.0);
    }

    /**
     * DEF-MC-1 等价测：单次聚合查询驱动的每个 gauge 与独立 derived-query oracle 逐一相等，
     * 覆盖全部 5 lifecycle 状态（distinct 计数防列序错位）+ blocked，并钉死「5 lifecycle 之和 ==
     * 总行数」与「blocked ⊆ lifecycle 总体」正交性不变量（santa CONCERN-2 加固）。
     */
    @Test
    void aggregateGauges_matchPerStatusDerivedQueries_acrossAllStates() {
        int k = 10000000;
        // CREATED = 1（非 blocked）
        service.create(String.valueOf(k++), "3101", "Q");
        // SENT = 4：2 非 blocked + 2 blocked(3115)，distinct 计数
        for (int i = 0; i < 2; i++) {
            final String key = String.valueOf(k++);
            service.create(key, "3101", "Q");
            service.markSent(key);
        }
        for (int i = 0; i < 2; i++) {
            final String key = String.valueOf(k++);
            service.create(key, "3115", "Q");        // correlation_blocked，停留 SENT
            service.markSent(key);
        }
        // RESULT_RECEIVED = 3
        for (int i = 0; i < 3; i++) {
            final String key = String.valueOf(k++);
            service.create(key, "3101", "Q");
            service.markSent(key);
            service.markResultReceived(key, "S" + key, key);
        }
        // FAILED = 5
        for (int i = 0; i < 5; i++) {
            final String key = String.valueOf(k++);
            service.create(key, "3101", "Q");
            service.markFailed(key);
        }
        // STUCK = 6
        for (int i = 0; i < 6; i++) {
            final String key = String.valueOf(k++);
            service.create(key, "3101", "Q");
            service.markStuck(key);
        }

        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics.bindTo(reg);

        // 每个 lifecycle gauge == 独立 derived query（聚合语义等价 oracle）
        long sumLifecycle = 0L;
        for (final RequestStateLifecycle s : RequestStateLifecycle.values()) {
            final double g = reg.find(RequestStateMetrics.GAUGE_COUNT)
                    .tag("status", s.name()).gauge().value();
            assertThat(g).isEqualTo((double) repository.countByLifecycleStatus(s));
            sumLifecycle += (long) g;
        }
        // blocked gauge == 独立 derived query（兼验 `= true` 布尔谓词方言正确性）
        final double blocked = reg.find(RequestStateMetrics.GAUGE_BLOCKED_COUNT).gauge().value();
        assertThat(blocked).isEqualTo((double) repository.countByCorrelationBlockedTrue());
        assertThat(blocked).isEqualTo(2.0);

        // 不变量：5 个 lifecycle gauge 之和 == 总行数（不含 blocked，正交）
        assertThat(sumLifecycle).isEqualTo(repository.count());
        // 正交性加固（santa CONCERN-2）：blocked ⊆ lifecycle 总体
        assertThat((long) blocked).isLessThanOrEqualTo(sumLifecycle);

        // 列序正确性（distinct 计数 1/4/3/5/6 防列错位）
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "CREATED").gauge().value()).isEqualTo(1.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "SENT").gauge().value()).isEqualTo(4.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "RESULT_RECEIVED").gauge().value()).isEqualTo(3.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "FAILED").gauge().value()).isEqualTo(5.0);
        assertThat(reg.find(RequestStateMetrics.GAUGE_COUNT)
                .tag("status", "STUCK").gauge().value()).isEqualTo(6.0);
    }
}
