package com.puchain.fep.collector.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link InProcessDistributedLock} 单元测试。
 *
 * <p>覆盖 Plan §T1b #4 强制场景：
 * <ul>
 *   <li>并发互斥（同 key 第二次 tryLock 返 Optional.empty）</li>
 *   <li>TTL 过期重新获取（注入 mutable Clock，避免 Thread.sleep）</li>
 *   <li>release token 不匹配不释放</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class DistributedLockTest {

    /** 标准 TTL（毫秒）—— 单元测试用。 */
    private static final long TTL_MS = 100L;

    /** 起始时刻（固定 Instant）。 */
    private static final Instant START = Instant.parse("2026-04-30T00:00:00Z");

    /** 并发测试线程数。 */
    private static final int CONCURRENT_THREADS = 32;

    @Test
    void tryLockShouldSucceedWhenKeyFree() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        Optional<LockToken> token = lock.tryLock("k1", TTL_MS);

        assertThat(token)
                .as("空闲 key 第一次 tryLock 必须成功")
                .isPresent();
        assertThat(token.get().key()).isEqualTo("k1");
        assertThat(token.get().ttlMillis()).isEqualTo(TTL_MS);
    }

    @Test
    void tryLockShouldFailWhenKeyHeld() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        Optional<LockToken> first = lock.tryLock("k1", TTL_MS);
        Optional<LockToken> second = lock.tryLock("k1", TTL_MS);

        assertThat(first).isPresent();
        assertThat(second)
                .as("同 key 第二次 tryLock 必须返 Optional.empty（互斥语义）")
                .isEmpty();
    }

    @Test
    void tryLockShouldSucceedAfterTtlExpiry() {
        AtomicReference<Clock> clockRef = new AtomicReference<>(Clock.fixed(START, ZoneOffset.UTC));
        InProcessDistributedLock lock = new InProcessDistributedLock(new MutableClock(clockRef));

        Optional<LockToken> first = lock.tryLock("k1", TTL_MS);
        assertThat(first).isPresent();

        // 推进时钟越过 TTL（+1ms 余量）
        clockRef.set(Clock.fixed(START.plusMillis(TTL_MS + 1), ZoneOffset.UTC));

        Optional<LockToken> second = lock.tryLock("k1", TTL_MS);
        assertThat(second)
                .as("TTL 过期后 tryLock 必须重新获取成功")
                .isPresent();
        assertThat(second.get().token())
                .as("新 token 必须与过期 token 不同（独立 UUID）")
                .isNotEqualTo(first.get().token());
    }

    @Test
    void tryLockShouldFailJustBeforeTtlExpiry() {
        AtomicReference<Clock> clockRef = new AtomicReference<>(Clock.fixed(START, ZoneOffset.UTC));
        InProcessDistributedLock lock = new InProcessDistributedLock(new MutableClock(clockRef));

        lock.tryLock("k1", TTL_MS);

        // 时钟刚好到达 TTL 边界，但未越过（边界即未过期）
        clockRef.set(Clock.fixed(START.plusMillis(TTL_MS), ZoneOffset.UTC));

        assertThat(lock.tryLock("k1", TTL_MS))
                .as("TTL 边界（now == acquiredAt + ttl）尚未过期，第二次 tryLock 必须失败")
                .isEmpty();
    }

    @Test
    void releaseWithMatchingTokenShouldFreeKey() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        LockToken token = lock.tryLock("k1", TTL_MS).orElseThrow();
        lock.release(token);

        Optional<LockToken> reAcquired = lock.tryLock("k1", TTL_MS);
        assertThat(reAcquired)
                .as("释放后同 key 必须可立即重新获取")
                .isPresent();
    }

    @Test
    void releaseWithWrongTokenShouldNotFreeKey() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        lock.tryLock("k1", TTL_MS);
        LockToken fake = new LockToken("k1", UUID.randomUUID().toString(), 0L, TTL_MS);
        lock.release(fake);

        assertThat(lock.tryLock("k1", TTL_MS))
                .as("token 不匹配的 release 必须不释放（持有人校验语义）")
                .isEmpty();
    }

    @Test
    void releaseWithNonExistentKeyShouldBeNoop() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        LockToken orphan = new LockToken("never_locked", UUID.randomUUID().toString(), 0L, TTL_MS);

        // 仅验证不抛异常（distributed semantics: silently no-op）
        lock.release(orphan);

        assertThat(lock.tryLock("never_locked", TTL_MS))
                .as("从未上锁的 key release 后应仍可获取")
                .isPresent();
    }

    @Test
    void tryLockShouldRejectNullKey() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        assertThatThrownBy(() -> lock.tryLock(null, TTL_MS))
                .as("null key 必须被拒（IllegalArgumentException）")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    @Test
    void tryLockShouldRejectEmptyKey() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        assertThatThrownBy(() -> lock.tryLock("", TTL_MS))
                .as("空字符串 key 必须被拒（IllegalArgumentException）")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    @Test
    void tryLockShouldRejectNonPositiveTtl() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        assertThatThrownBy(() -> lock.tryLock("k1", 0L))
                .as("ttlMillis=0 必须被拒")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlMillis");
        assertThatThrownBy(() -> lock.tryLock("k1", -1L))
                .as("ttlMillis<0 必须被拒")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlMillis");
    }

    @Test
    void releaseNullTokenShouldThrowNpe() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        assertThatThrownBy(() -> lock.release(null))
                .as("release(null) 必须抛 NullPointerException")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }

    @Test
    void lockTokenShouldRejectNonPositiveTtl() {
        String token = UUID.randomUUID().toString();

        assertThatThrownBy(() -> new LockToken("k", token, 0L, 0L))
                .as("LockToken ttlMillis=0 必须被拒")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlMillis");
        assertThatThrownBy(() -> new LockToken("k", token, 0L, -1L))
                .as("LockToken ttlMillis<0 必须被拒")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttlMillis");
    }

    @Test
    void lockTokenShouldRejectNullKeyAndToken() {
        String token = UUID.randomUUID().toString();

        assertThatThrownBy(() -> new LockToken(null, token, 0L, TTL_MS))
                .as("LockToken null key 必须被拒")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key");
        assertThatThrownBy(() -> new LockToken("k", null, 0L, TTL_MS))
                .as("LockToken null token 必须被拒")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("token");
    }

    @Test
    void differentKeysAreIndependent() {
        InProcessDistributedLock lock = new InProcessDistributedLock();

        Optional<LockToken> tokenA = lock.tryLock("k1", TTL_MS);
        Optional<LockToken> tokenB = lock.tryLock("k2", TTL_MS);

        assertThat(tokenA).as("k1 必须可获取").isPresent();
        assertThat(tokenB).as("k2 必须独立可获取（不同 key 不互斥）").isPresent();
    }

    /**
     * 并发互斥 smoke 测试：N 线程同时 tryLock 同一 key，仅 1 个成功。
     */
    @Test
    void concurrentTryLockShouldYieldExactlyOneWinner() throws InterruptedException {
        InProcessDistributedLock lock = new InProcessDistributedLock();
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_THREADS);
        AtomicInteger winners = new AtomicInteger();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (lock.tryLock("contested", TTL_MS).isPresent()) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean allDone = done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(allDone).as("并发任务必须在 5 秒内完成").isTrue();
        assertThat(winners.get())
                .as("N 个线程争抢同一 key 必须仅 1 个赢家（互斥）")
                .isEqualTo(1);
    }

    /**
     * 测试用 mutable Clock —— 通过 AtomicReference 切换底层 fixed Clock，
     * 实现"快进时间"语义而无需 Thread.sleep。
     */
    private static final class MutableClock extends Clock {
        private final AtomicReference<Clock> ref;

        MutableClock(final AtomicReference<Clock> ref) {
            this.ref = ref;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ref.get().getZone();
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return ref.get().withZone(zone);
        }

        @Override
        public Instant instant() {
            return ref.get().instant();
        }

        @Override
        public long millis() {
            return ref.get().millis();
        }
    }
}
