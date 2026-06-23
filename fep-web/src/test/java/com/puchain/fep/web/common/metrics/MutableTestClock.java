package com.puchain.fep.web.common.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可前进的测试时钟：替代各 metrics 测试中重复的匿名 {@link Clock} 子类样板（DEF-MC-2）。
 *
 * <p>{@link #advance(Duration)} 推进当前时刻，使 {@code CachedSupplier} 等 TTL 窗逻辑可被
 * 确定性地驱动。线程安全（{@link AtomicReference}），但测试用法通常单线程。</p>
 */
public final class MutableTestClock extends Clock {

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    /**
     * @param start 初始时刻
     */
    public MutableTestClock(final Instant start) {
        this(new AtomicReference<>(start), ZoneOffset.UTC);
    }

    private MutableTestClock(final AtomicReference<Instant> now, final ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    /**
     * 前进当前时刻。
     *
     * @param delta 前进量
     */
    public void advance(final Duration delta) {
        now.updateAndGet(current -> current.plus(delta));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(final ZoneId newZone) {
        return new MutableTestClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
