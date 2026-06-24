package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.audit.review.config.ReviewWorkflowProperties;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.metrics.AuditReviewMetrics;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * {@link MessageReviewTaskService} 三个失败 throw 点的失败 metrics 打点验证（纯 Mockito，
 * 持有真 {@link SimpleMeterRegistry} 读计数）。
 *
 * <p>覆盖 {@code fep_audit_review_failure_total} 的三个 reason 维度：{@code not_found}
 * （任务不存在 BIZ_5001）/ {@code terminal}（已终态 BIZ_5003）/ {@code lock_conflict}
 * （并发乐观锁冲突 BIZ_5003）。BIZ_5003 被 terminal 与 lock_conflict 复用，故须按不同
 * throw 点打不同 reason tag。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MessageReviewTaskServiceFailureMetricsTest {

    private final MessageReviewTaskRepository repository = mock(MessageReviewTaskRepository.class);
    private final ReviewWorkflowProperties properties = new ReviewWorkflowProperties();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AuditReviewMetrics metrics =
            new AuditReviewMetrics(registry, Clock.systemUTC(), Duration.ofSeconds(10));
    private final MessageReviewTaskService service =
            new MessageReviewTaskService(repository, properties, metrics);

    private double failureCount(final String reason) {
        return registry.counter("fep_audit_review_failure_total", "reason", reason).count();
    }

    private static MessageReviewTaskEntity task(final String id, final String status) {
        final MessageReviewTaskEntity t = new MessageReviewTaskEntity();
        t.setReviewId(id);
        t.setReviewStatus(status);
        t.setReviewLevel(1);
        t.setCurrentLevel(1);
        return t;
    }

    @Test
    void approve_notFound_recordsNotFoundFailure() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve("missing", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_NOT_FOUND)).isEqualTo(1.0);
    }

    @Test
    void approve_terminal_recordsTerminalFailure() {
        when(repository.findById("done")).thenReturn(
                Optional.of(task("done", ReviewStatus.APPROVED.name())));

        assertThatThrownBy(() -> service.approve("done", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_TERMINAL)).isEqualTo(1.0);
    }

    @Test
    void approve_lockConflict_recordsLockConflictFailure() {
        when(repository.findById("rev")).thenReturn(
                Optional.of(task("rev", ReviewStatus.PENDING.name())));
        when(repository.saveAndFlush(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(MessageReviewTaskEntity.class, "rev"));

        assertThatThrownBy(() -> service.approve("rev", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_LOCK_CONFLICT)).isEqualTo(1.0);
    }

    @Test
    void reject_notFound_recordsNotFoundFailure() {
        when(repository.findById("missing2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject("missing2", "rv", "bad"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_NOT_FOUND)).isEqualTo(1.0);
    }

    @Test
    void reject_terminal_recordsTerminalFailure() {
        when(repository.findById("done2")).thenReturn(
                Optional.of(task("done2", ReviewStatus.REJECTED.name())));

        assertThatThrownBy(() -> service.reject("done2", "rv", "bad"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_TERMINAL)).isEqualTo(1.0);
    }

    @Test
    void reject_lockConflict_recordsLockConflictFailure() {
        when(repository.findById("rev2")).thenReturn(
                Optional.of(task("rev2", ReviewStatus.PENDING.name())));
        when(repository.saveAndFlush(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(MessageReviewTaskEntity.class, "rev2"));

        assertThatThrownBy(() -> service.reject("rev2", "rv", "bad"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount(AuditReviewMetrics.REASON_LOCK_CONFLICT)).isEqualTo(1.0);
    }
}
