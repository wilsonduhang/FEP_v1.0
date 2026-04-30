package com.puchain.fep.collector.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CollectionMetrics} 单元测试。
 *
 * <p>覆盖 Plan §T1b #4 第 4 条：CollectionMetrics 多线程并发 inc 后 snapshot 正确
 * （ExecutorService 100 task × 10 inc）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>5 个 incXxx 方法分别更新对应字段，互不影响</li>
 *   <li>snapshot() 返回不可变 record，反映当前累计值</li>
 *   <li>并发 inc 不丢失更新（LongAdder 语义）</li>
 *   <li>负 delta 允许（与 LongAdder.add 一致，用于补偿场景）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectionMetricsTest {

    /** 并发测试线程数。 */
    private static final int CONCURRENT_THREADS = 100;

    /** 每线程操作次数。 */
    private static final int OPS_PER_THREAD = 10;

    /** 并发完成等待时长（秒）。 */
    private static final int AWAIT_SECONDS = 10;

    @Test
    void incCollectedShouldUpdateSnapshot() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incCollected(5L);
        metrics.incCollected(3L);

        CollectionMetricsSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.collected())
                .as("incCollected 累计值必须正确")
                .isEqualTo(8L);
    }

    @Test
    void incAssembledShouldUpdateSnapshot() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incAssembled(7L);

        assertThat(metrics.snapshot().assembled()).isEqualTo(7L);
    }

    @Test
    void incSubmittedShouldUpdateSnapshot() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incSubmitted(11L);

        assertThat(metrics.snapshot().submitted()).isEqualTo(11L);
    }

    @Test
    void incFailedShouldUpdateSnapshot() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incFailed(13L);

        assertThat(metrics.snapshot().failed()).isEqualTo(13L);
    }

    @Test
    void incSkippedShouldUpdateSnapshot() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incSkipped(17L);

        assertThat(metrics.snapshot().skipped()).isEqualTo(17L);
    }

    @Test
    void incrementsAreIndependent() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incCollected(1L);

        CollectionMetricsSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.collected()).as("incCollected 仅影响 collected 字段").isEqualTo(1L);
        assertThat(snapshot.assembled()).as("不应影响 assembled").isZero();
        assertThat(snapshot.submitted()).as("不应影响 submitted").isZero();
        assertThat(snapshot.failed()).as("不应影响 failed").isZero();
        assertThat(snapshot.skipped()).as("不应影响 skipped").isZero();
    }

    @Test
    void initialSnapshotShouldBeAllZero() {
        CollectionMetrics metrics = new CollectionMetrics();

        CollectionMetricsSnapshot snapshot = metrics.snapshot();

        assertThat(snapshot.collected()).isZero();
        assertThat(snapshot.assembled()).isZero();
        assertThat(snapshot.submitted()).isZero();
        assertThat(snapshot.failed()).isZero();
        assertThat(snapshot.skipped()).isZero();
    }

    /**
     * 负 delta 场景：补偿/回退场景下允许负值。LongAdder.add 接受 long，
     * 不主动校验正负 — 由调用方业务语义决定。
     */
    @Test
    void negativeDeltaIsAllowed() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incCollected(10L);
        metrics.incCollected(-3L);

        assertThat(metrics.snapshot().collected())
                .as("负 delta 必须允许（用于补偿场景，与 LongAdder.add 一致）")
                .isEqualTo(7L);
    }

    /**
     * Plan §T1b #4 强制场景：100 线程 × 10 inc 并发，snapshot 总和必须等于
     * threadCount × opsPerThread = 1000，无丢失更新。
     */
    @Test
    void concurrentIncShouldNotLoseUpdates() throws InterruptedException {
        CollectionMetrics metrics = new CollectionMetrics();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int op = 0; op < OPS_PER_THREAD; op++) {
                        metrics.incCollected(1L);
                        metrics.incAssembled(1L);
                        metrics.incSubmitted(1L);
                        metrics.incFailed(1L);
                        metrics.incSkipped(1L);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.incrementAndGet();
                } catch (RuntimeException e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean allDone = done.await(AWAIT_SECONDS, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(allDone).as("并发任务必须在 " + AWAIT_SECONDS + " 秒内完成").isTrue();
        assertThat(failures.get()).as("并发期间不得抛异常").isZero();

        long expected = (long) CONCURRENT_THREADS * OPS_PER_THREAD;
        CollectionMetricsSnapshot snapshot = metrics.snapshot();
        assertThat(snapshot.collected())
                .as("collected 并发累计必须等于 threads × ops（无丢失更新）")
                .isEqualTo(expected);
        assertThat(snapshot.assembled()).isEqualTo(expected);
        assertThat(snapshot.submitted()).isEqualTo(expected);
        assertThat(snapshot.failed()).isEqualTo(expected);
        assertThat(snapshot.skipped()).isEqualTo(expected);
    }

    @Test
    void snapshotShouldBeImmutablePointInTime() {
        CollectionMetrics metrics = new CollectionMetrics();

        metrics.incCollected(1L);
        CollectionMetricsSnapshot first = metrics.snapshot();
        metrics.incCollected(99L);
        CollectionMetricsSnapshot second = metrics.snapshot();

        assertThat(first.collected())
                .as("snapshot 必须是 point-in-time 不可变快照，后续 inc 不影响早期快照")
                .isEqualTo(1L);
        assertThat(second.collected()).isEqualTo(100L);
    }
}
