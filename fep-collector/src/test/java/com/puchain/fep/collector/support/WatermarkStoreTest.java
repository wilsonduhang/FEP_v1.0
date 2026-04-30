package com.puchain.fep.collector.support;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InMemoryWatermarkStore} 单元测试，覆盖 {@link WatermarkStore} 契约。
 *
 * <p>覆盖：
 * <ul>
 *   <li>未知 adapterId 返回 {@link Optional#empty()}</li>
 *   <li>put 后 get 返回相同值</li>
 *   <li>put 覆盖：第二次 put 替换第一次的值</li>
 *   <li>并发安全 smoke：100 线程 × 10 ops 同时 put/get 不抛异常且最终值合法</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class WatermarkStoreTest {

    @Test
    void getUnknownAdapterIdShouldReturnEmpty() {
        WatermarkStore store = new InMemoryWatermarkStore();

        assertThat(store.get("ADP_UNKNOWN"))
                .as("未知 adapterId 必须返回 Optional.empty（首次采集语义）")
                .isEmpty();
    }

    @Test
    void putThenGetShouldReturnSameValue() {
        WatermarkStore store = new InMemoryWatermarkStore();

        store.put("ADP1", "2026-04-30T00:00:00Z");

        assertThat(store.get("ADP1"))
                .as("put 后 get 必须返回相同水位值")
                .contains("2026-04-30T00:00:00Z");
    }

    @Test
    void putShouldOverwritePreviousValue() {
        WatermarkStore store = new InMemoryWatermarkStore();

        store.put("ADP1", "2026-04-29T00:00:00Z");
        store.put("ADP1", "2026-04-30T00:00:00Z");

        assertThat(store.get("ADP1"))
                .as("第二次 put 必须覆盖第一次的值（水位推进语义）")
                .contains("2026-04-30T00:00:00Z");
    }

    @Test
    void putShouldNotAffectOtherAdapterIds() {
        WatermarkStore store = new InMemoryWatermarkStore();

        store.put("ADP1", "wm1");
        store.put("ADP2", "wm2");

        assertThat(store.get("ADP1")).contains("wm1");
        assertThat(store.get("ADP2")).contains("wm2");
    }

    /**
     * 并发安全 smoke 测试：100 线程并发 put/get 同一 adapterId，
     * 验证 ConcurrentHashMap 后端无 ConcurrentModificationException 且最终值
     * 来自实际写入集（非 null / 非任意脏读）。
     */
    @Test
    void concurrentPutGetShouldBeSafe() throws InterruptedException {
        WatermarkStore store = new InMemoryWatermarkStore();
        int threadCount = 100;
        int opsPerThread = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger();

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int op = 0; op < opsPerThread; op++) {
                        store.put("ADP_SHARED", "wm-" + threadIdx + "-" + op);
                        Optional<String> read = store.get("ADP_SHARED");
                        if (read.isEmpty() || !read.get().startsWith("wm-")) {
                            failures.incrementAndGet();
                        }
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
        boolean allDone = done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(allDone)
                .as("100 线程 × 10 ops 必须在 10 秒内完成（无死锁）")
                .isTrue();
        assertThat(failures.get())
                .as("并发 put/get 期间不得出现异常或读到脏数据")
                .isZero();
        assertThat(store.get("ADP_SHARED"))
                .as("最终值必须来自实际写入集（非 null / 非脏读）")
                .isPresent()
                .get().asString().startsWith("wm-");
    }
}
