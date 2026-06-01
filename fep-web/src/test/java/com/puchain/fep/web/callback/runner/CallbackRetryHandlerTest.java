package com.puchain.fep.web.callback.runner;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CallbackRetryHandler}: 4xx classification, exp backoff,
 * per-interface maxAttempts override, and DLQ exhaustion.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackRetryHandlerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 26, 10, 0, 0);

    private CallbackQueueRepository repo;
    private ApplicationEventPublisher eventPublisher;
    private CallbackRetryHandler handler;

    /**
     * @return default properties; global fallback maxAttempts=3 (PRD §5.5.2).
     */
    private static CallbackQueueProperties defaultProps() {
        return new CallbackQueueProperties(50, 5000L,
                new CallbackQueueProperties.Retry(30000L, 1800000L, 3));
    }

    /**
     * Helper: create a PENDING entity and seed it with {@code count} RETRY transitions
     * so that {@code entity.getRetryCount() == count} before the test call.
     *
     * @param count desired retryCount before the handler call under test
     * @return entity with retryCount == count
     */
    private static CallbackQueueEntity pendingWithRetryCount(final int count) {
        final CallbackQueueEntity e =
                CallbackQueueEntity.pending("idem", "iface-1", "3001", "{}");
        for (int i = 0; i < count; i++) {
            e.markRetry(i + 1, FIXED_NOW, "prev");
        }
        return e;
    }

    @BeforeEach
    void setUp() {
        repo = mock(CallbackQueueRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        final Clock clock = Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE);
        handler = new CallbackRetryHandler(repo, defaultProps(), clock, eventPublisher);
    }

    /**
     * 4xx (e.g. 403) must go DEAD_LETTER immediately without scheduling a retry.
     * retryCount 0 → 1, nextRetryAt=null, outcome=DEAD_LETTER.
     */
    @Test
    void clientError4xx_shouldGoDeadLetterImmediately() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 403, "http 403"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(1);
        assertThat(e.getNextRetryAt()).isNull();
        verify(repo).save(e);
    }

    /**
     * 5xx below per-interface maxAttempts=5 must produce RETRY with exp backoff.
     * retryCount 0→1; shift=min(1,30)=1; backoff=min(30000<<1,1800000)=60000ms=60s.
     */
    @Test
    void serverError5xx_belowMax_shouldRetryWithBackoff() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 500, "http 500"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.RETRY);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.RETRY);
        assertThat(e.getRetryCount()).isEqualTo(1);
        // shift=min(1,30)=1; backoff=min(30000<<1,1800000)=60000ms
        assertThat(e.getNextRetryAt()).isEqualTo(FIXED_NOW.plusSeconds(60));
    }

    /**
     * IO failure (statusCode=0) at maxAttempts must go DEAD_LETTER.
     * retryCount 4→5; 5>=5 → DEAD_LETTER, nextRetryAt=null.
     */
    @Test
    void ioFailure_atMax_shouldGoDeadLetter() {
        final CallbackQueueEntity e = pendingWithRetryCount(4); // retryCount=4 → +1=5
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 0, "io: timeout"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(5);
        assertThat(e.getNextRetryAt()).isNull();
    }

    /**
     * Per-interface retryCount=2 must override global default (3).
     * retryCount 1→2; 2>=2 → DEAD_LETTER.
     */
    @Test
    void perInterfaceRetryCount_shouldOverrideGlobalDefault() {
        final CallbackQueueEntity e = pendingWithRetryCount(1); // retryCount=1 → +1=2
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 2, new CallbackResult(false, 503, "http 503"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER); // 2>=2
    }

    /**
     * Zero interfaceRetryCount must fall back to global default maxAttempts=3 (PRD §5.5.2).
     * retryCount 0→1; 1<3 → RETRY.
     */
    @Test
    void zeroInterfaceRetryCount_shouldFallBackToGlobalDefault() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 0, new CallbackResult(false, 500, "http 500"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.RETRY); // 1<3 (global default fallback)
    }

    /**
     * Backoff must cap at maxBackoffMillis (1800000ms=30min).
     * retryCount 9→10; shift=min(10,30)=10; 30000<<10=30720000 > 1800000 → cap → nextRetryAt +1800s.
     */
    @Test
    void backoff_shouldCapAtMaxBackoff() {
        final CallbackQueueEntity e = pendingWithRetryCount(9); // retryCount=9 → +1=10
        final ArgumentCaptor<CallbackQueueEntity> cap = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        handler.handleDeliveryFailure(e, 100, new CallbackResult(false, 500, "http 500"));
        verify(repo).save(cap.capture());
        // shift=min(10,30)=10; 30000<<10=30720000 > 1800000 → cap 1800000ms=30min
        assertThat(cap.getValue().getNextRetryAt()).isEqualTo(FIXED_NOW.plusSeconds(1800));
    }

    /**
     * DEAD_LETTER (T10) must publish CallbackDeadLetterEvent carrying queue identity +
     * retryCount + error for InAppNotificationListener (T12) to consume.
     */
    @Test
    void deadLetter_shouldPublishDeadLetterEvent() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 403, "http 403"));

        final ArgumentCaptor<CallbackDeadLetterEvent> cap =
                ArgumentCaptor.forClass(CallbackDeadLetterEvent.class);
        verify(eventPublisher).publishEvent(cap.capture());
        final CallbackDeadLetterEvent ev = cap.getValue();
        assertThat(ev.queueId()).isEqualTo(e.getQueueId());
        assertThat(ev.targetInterfaceId()).isEqualTo("iface-1");
        assertThat(ev.msgNo()).isEqualTo("3001");
        assertThat(ev.retryCount()).isEqualTo(1);
        assertThat(ev.lastError()).isEqualTo("http 403");
        assertThat(ev.occurredAt()).isEqualTo(FIXED_NOW);
    }

    /**
     * RETRY path must NOT publish any DEAD_LETTER event (event only on terminal DLQ).
     */
    @Test
    void retry_shouldNotPublishDeadLetterEvent() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 500, "http 500"));
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
