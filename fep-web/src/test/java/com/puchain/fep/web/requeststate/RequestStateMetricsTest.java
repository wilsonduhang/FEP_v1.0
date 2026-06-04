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
}
