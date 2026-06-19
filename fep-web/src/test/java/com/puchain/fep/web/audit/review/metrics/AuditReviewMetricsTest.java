package com.puchain.fep.web.audit.review.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * {@link AuditReviewMetrics} 组件单测（独立 {@link SimpleMeterRegistry}，精确值断言）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class AuditReviewMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AuditReviewMetrics metrics = new AuditReviewMetrics(registry);

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
    void pendingGauge_reflectsSupplierLive() {
        final AtomicInteger pending = new AtomicInteger(5);
        metrics.registerPendingGauge(pending::get);

        assertThat(registry.get("fep_audit_review_pending_count").gauge().value()).isEqualTo(5.0);

        pending.set(2);
        assertThat(registry.get("fep_audit_review_pending_count").gauge().value()).isEqualTo(2.0);
    }
}
