package com.puchain.fep.web.callback.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackQueueEntityTest {

    private static CallbackQueueEntity newPending() {
        return CallbackQueueEntity.pending("idem-key", "iface-1", "3001", "{\"k\":\"v\"}");
    }

    private static CallbackQueueEntity newDeadLetter() {
        final CallbackQueueEntity e = CallbackQueueEntity.pending(
                "idem-dlq", "iface-9", "3009", "{\"payload\":\"x\"}");
        e.markRetry(4, LocalDateTime.now(), "prev");
        e.markDeadLetter(5, "fatal");
        return e;
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

    @Test
    void copyForReplay_shouldCreateNewPendingRowLinkingOriginal() {
        final CallbackQueueEntity dead = newDeadLetter();

        final CallbackQueueEntity replay = CallbackQueueEntity.copyForReplay(dead, "admin-user-x");

        assertThat(replay.getQueueId()).isNotEqualTo(dead.getQueueId());
        assertThat(replay.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(replay.getRetryCount()).isZero();
        assertThat(replay.getNextRetryAt()).isNull();
        assertThat(replay.getOriginalDlqId()).isEqualTo(dead.getQueueId());
        assertThat(replay.getReplayedBy()).isEqualTo("admin-user-x");
        assertThat(replay.getReplayedAt()).isNotNull();
        assertThat(replay.getTargetInterfaceId()).isEqualTo(dead.getTargetInterfaceId());
        assertThat(replay.getMsgNo()).isEqualTo(dead.getMsgNo());
        assertThat(replay.getPayloadJson()).isEqualTo(dead.getPayloadJson());
        assertThat(replay.getIdempotencyKey()).startsWith(dead.getIdempotencyKey() + "-RPL-");
    }

    @Test
    void copyForReplay_shouldLeaveOriginalUnchanged() {
        final CallbackQueueEntity dead = newDeadLetter();
        final String originalKey = dead.getIdempotencyKey();

        CallbackQueueEntity.copyForReplay(dead, "admin-user-x");

        assertThat(dead.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(dead.getRetryCount()).isEqualTo(5);
        assertThat(dead.getIdempotencyKey()).isEqualTo(originalKey);
        assertThat(dead.getOriginalDlqId()).isNull();
        assertThat(dead.getReplayedBy()).isNull();
    }

    @Test
    void copyForReplay_shouldRejectNonDeadLetterSource() {
        final CallbackQueueEntity pending = newPending();
        assertThatThrownBy(() -> CallbackQueueEntity.copyForReplay(pending, "u"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEAD_LETTER");
    }

    @Test
    void markAsStaleReclaim_shouldResetToPendingIncrementRetryAndClearClaimedAt() {
        final CallbackQueueEntity e = newPending();
        e.markSending();
        assertThat(e.getClaimedAt()).isNotNull();

        e.markAsStaleReclaim();

        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(e.getRetryCount()).isEqualTo(1);
        assertThat(e.getClaimedAt()).isNull();
    }
}
