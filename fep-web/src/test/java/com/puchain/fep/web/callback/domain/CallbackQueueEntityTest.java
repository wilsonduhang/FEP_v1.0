package com.puchain.fep.web.callback.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackQueueEntityTest {

    private static CallbackQueueEntity newPending() {
        return CallbackQueueEntity.pending("idem-key", "iface-1", "3001", "{\"k\":\"v\"}");
    }

    @Test
    void pending_shouldStartWithZeroRetryAndNullScheduling() {
        final CallbackQueueEntity e = newPending();
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(e.getRetryCount()).isZero();
        assertThat(e.getNextRetryAt()).isNull();
        assertThat(e.getClaimedAt()).isNull();
    }

    @Test
    void markSending_shouldSetSendingStatusAndClaimedAt() {
        final CallbackQueueEntity e = newPending();
        e.markSending();
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.SENDING);
        assertThat(e.getClaimedAt()).isNotNull();
    }

    @Test
    void markRetry_shouldSetRetryStatusCountAndNextRetryAt() {
        final CallbackQueueEntity e = newPending();
        final LocalDateTime next = LocalDateTime.of(2026, 5, 26, 10, 0, 0);
        e.markRetry(3, next, "boom");
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.RETRY);
        assertThat(e.getRetryCount()).isEqualTo(3);
        assertThat(e.getNextRetryAt()).isEqualTo(next);
        assertThat(e.getLastError()).isEqualTo("boom");
    }

    @Test
    void markDeadLetter_shouldSetDeadLetterClearNextRetryAt() {
        final CallbackQueueEntity e = newPending();
        e.markRetry(4, LocalDateTime.now(), "prev");
        e.markDeadLetter(5, "fatal");
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(5);
        assertThat(e.getNextRetryAt()).isNull();
        assertThat(e.getLastError()).isEqualTo("fatal");
    }

    @Test
    void markRetry_shouldTruncateErrorTo500() {
        final CallbackQueueEntity e = newPending();
        e.markRetry(1, LocalDateTime.now(), "x".repeat(600));
        assertThat(e.getLastError()).hasSize(500);
    }
}
