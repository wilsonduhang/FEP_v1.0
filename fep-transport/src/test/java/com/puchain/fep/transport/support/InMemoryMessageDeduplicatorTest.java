package com.puchain.fep.transport.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * Unit tests for {@link InMemoryMessageDeduplicator}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InMemoryMessageDeduplicatorTest {

    @Test
    void isDuplicate_nullMsgId_shouldThrowNpe() {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(100);

        assertThatNullPointerException()
                .isThrownBy(() -> dedup.isDuplicate(null))
                .withMessageContaining("msgId");
    }

    @Test
    void isDuplicate_firstCall_shouldReturnFalse() {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(100);

        assertThat(dedup.isDuplicate("MSG001")).isFalse();
    }

    @Test
    void isDuplicate_secondCall_shouldReturnTrue() {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(100);

        dedup.isDuplicate("MSG001");

        assertThat(dedup.isDuplicate("MSG001")).isTrue();
    }

    @Test
    void isDuplicate_differentMsgIds_shouldReturnFalse() {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(100);

        assertThat(dedup.isDuplicate("MSG001")).isFalse();
        assertThat(dedup.isDuplicate("MSG002")).isFalse();
        assertThat(dedup.isDuplicate("MSG003")).isFalse();
    }

    @Test
    void isDuplicate_exceedsCapacity_shouldEvictOldest() {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(3);

        dedup.isDuplicate("MSG001");
        dedup.isDuplicate("MSG002");
        dedup.isDuplicate("MSG003");
        // Capacity is 3; adding a 4th should evict MSG001 (oldest)
        dedup.isDuplicate("MSG004");

        // MSG002, MSG003, MSG004 are still present
        assertThat(dedup.isDuplicate("MSG002")).isTrue();
        assertThat(dedup.isDuplicate("MSG003")).isTrue();
        assertThat(dedup.isDuplicate("MSG004")).isTrue();

        // MSG001 was evicted — treated as new
        assertThat(dedup.isDuplicate("MSG001")).isFalse();
    }

    @Test
    void isDuplicate_concurrentAccess_shouldNotLoseRecords() throws Exception {
        final MessageDeduplicator dedup = new InMemoryMessageDeduplicator(10_000);
        final int threadCount = 8;
        final int msgsPerThread = 500;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService exec = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            exec.submit(() -> {
                for (int i = 0; i < msgsPerThread; i++) {
                    dedup.isDuplicate("T" + threadId + "_MSG" + i);
                }
                latch.countDown();
            });
        }
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        exec.shutdown();

        // All messages should now be duplicates
        for (int t = 0; t < threadCount; t++) {
            for (int i = 0; i < msgsPerThread; i++) {
                assertThat(dedup.isDuplicate("T" + t + "_MSG" + i)).isTrue();
            }
        }
    }
}
