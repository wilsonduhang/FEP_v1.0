package com.puchain.fep.web.audit.review.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.common.metrics.MutableTestClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * {@link AuditReviewMetrics} 组件单测（独立 {@link SimpleMeterRegistry}，精确值断言）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class AuditReviewMetricsTest {

    private static final Duration TTL = Duration.ofSeconds(10);

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    /** 可前进的测试时钟。 */
    private final MutableTestClock clock =
            new MutableTestClock(Instant.parse("2026-06-20T00:00:00Z"));

    private final AuditReviewMetrics metrics = new AuditReviewMetrics(registry, clock, TTL);

    @Test
    void recordApproved_incrementsApprovedCounter() {
        metrics.recordDecision("APPROVED");
        metrics.recordDecision("APPROVED");

        assertThat(registry.counter("fep_audit_review_decision_total", "decision", "APPROVED").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordRejected_incrementsRejectedCounterIndependently() {
        metrics.recordDecision("REJECTED");

        assertThat(registry.counter("fep_audit_review_decision_total", "decision", "REJECTED").count())
                .isEqualTo(1.0);
        // APPROVED tag 独立，未被 REJECTED 污染
        assertThat(registry.counter("fep_audit_review_decision_total", "decision", "APPROVED").count())
                .isEqualTo(0.0);
    }

    @Test
    void recordFailure_incrementsPerReasonCounterIndependently() {
        metrics.recordFailure(AuditReviewMetrics.REASON_NOT_FOUND);
        metrics.recordFailure(AuditReviewMetrics.REASON_LOCK_CONFLICT);
        metrics.recordFailure(AuditReviewMetrics.REASON_LOCK_CONFLICT);

        assertThat(registry.counter("fep_audit_review_failure_total", "reason", "not_found").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("fep_audit_review_failure_total", "reason", "lock_conflict").count())
                .isEqualTo(2.0);
        // terminal tag 独立，未被污染
        assertThat(registry.counter("fep_audit_review_failure_total", "reason", "terminal").count())
                .isEqualTo(0.0);
    }

    @Test
    void pendingGauge_cachesWithinTtlThenRefreshes() {
        final AtomicInteger pending = new AtomicInteger(5);
        metrics.registerPendingGauge(pending::get);

        assertThat(registry.get("fep_audit_review_pending_count").gauge().value()).isEqualTo(5.0);
        pending.set(2);
        clock.advance(Duration.ofSeconds(9));   // 窗内 → 复用陈旧值
        assertThat(registry.get("fep_audit_review_pending_count").gauge().value()).isEqualTo(5.0);
        clock.advance(Duration.ofSeconds(1));   // 达 TTL → 刷新
        assertThat(registry.get("fep_audit_review_pending_count").gauge().value()).isEqualTo(2.0);
    }
}
