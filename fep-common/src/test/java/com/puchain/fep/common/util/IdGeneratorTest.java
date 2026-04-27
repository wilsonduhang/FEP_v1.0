package com.puchain.fep.common.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IdGenerator}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class IdGeneratorTest {

    // --- uuid20 ---

    @Test
    void uuid20_shouldHaveExactly20Chars() {
        for (int i = 0; i < 100; i++) {
            assertThat(IdGenerator.uuid20()).hasSize(20);
        }
    }

    @Test
    void uuid20_shouldBeBase36() {
        assertThat(IdGenerator.uuid20()).matches("[0-9a-z]{20}");
    }

    @Test
    void uuid20_shouldBeUnique_in1000Iterations() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdGenerator.uuid20());
        }
        assertThat(ids).hasSize(1000);
    }

    @Test
    void uuid20_isThreadSafe() throws Exception {
        int threads = 16;
        int perThread = 500;
        Set<String> ids = ConcurrentHashMap.newKeySet();
        ExecutorService es = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch done = new CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                es.submit(() -> {
                    for (int j = 0; j < perThread; j++) {
                        ids.add(IdGenerator.uuid20());
                    }
                    done.countDown();
                });
            }
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(ids).hasSize(threads * perThread);
        } finally {
            es.shutdown();
        }
    }
}
