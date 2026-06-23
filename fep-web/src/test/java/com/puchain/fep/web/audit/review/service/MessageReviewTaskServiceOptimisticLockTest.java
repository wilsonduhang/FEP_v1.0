package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.puchain.fep.common.domain.FepErrorCode;
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
 * {@link MessageReviewTaskService} 决策路径乐观锁冲突翻译验证（纯 Mockito 单测，无 Spring 上下文）。
 *
 * <p>并发双决策场景：两审核人同时持 PENDING 任务的版本快照决策，先提交者把
 * {@code row_version} 推进，后提交者 {@code saveAndFlush} 命中 0 行 → Hibernate 抛
 * {@link ObjectOptimisticLockingFailureException}。service 须捕获并翻译为
 * {@link FepBusinessException}（{@code BIZ_5003}，→ HTTP 400，与终态守卫同语义），
 * 而非逃逸为 HTTP 500。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MessageReviewTaskServiceOptimisticLockTest {

    private final MessageReviewTaskRepository repository = mock(MessageReviewTaskRepository.class);
    private final ReviewWorkflowProperties properties = new ReviewWorkflowProperties();
    // AuditReviewMetrics 三参构造 (MeterRegistry, Clock, Duration)
    private final AuditReviewMetrics metrics = new AuditReviewMetrics(
            new SimpleMeterRegistry(), Clock.systemUTC(), Duration.ofSeconds(10));
    private final MessageReviewTaskService service =
            new MessageReviewTaskService(repository, properties, metrics);

    private static MessageReviewTaskEntity pending(final String id) {
        final MessageReviewTaskEntity t = new MessageReviewTaskEntity();
        t.setReviewId(id);
        t.setReviewStatus(ReviewStatus.PENDING.name());
        t.setReviewLevel(1);
        t.setCurrentLevel(1);
        return t;
    }

    @Test
    void approve_optimisticLockConflict_translatedToBiz5003() {
        when(repository.findById("rev-1")).thenReturn(Optional.of(pending("rev-1")));
        when(repository.saveAndFlush(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(MessageReviewTaskEntity.class, "rev-1"));

        assertThatThrownBy(() -> service.approve("rev-1", "reviewer-x", "ok"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.BIZ_5003);
    }

    @Test
    void reject_optimisticLockConflict_translatedToBiz5003() {
        when(repository.findById("rev-2")).thenReturn(Optional.of(pending("rev-2")));
        when(repository.saveAndFlush(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(MessageReviewTaskEntity.class, "rev-2"));

        assertThatThrownBy(() -> service.reject("rev-2", "reviewer-y", "bad"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.BIZ_5003);
    }
}
