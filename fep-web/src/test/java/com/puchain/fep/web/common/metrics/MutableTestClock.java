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

    /**
     * 返回同区不同的派生时钟。<strong>派生时钟与本时钟共享同一 {@code now}
     * 引用</strong>——对任一方 {@link #advance(Duration)} 对另一方立即可见。本类专用于
     * 单时钟测试，当前无消费方调用 {@code withZone}（仅 {@link Clock} 契约要求实现），
     * 故该共享无可观测影响；如未来需要相互独立的派生时钟，须改为深拷贝
     * {@link AtomicReference}（DEF-MC-3）。
     *
     * @param newZone 目标时区
     * @return 共享 {@code now}、时区为 {@code newZone} 的新实例
     */
    @Override
    public Clock withZone(final ZoneId newZone) {
        return new MutableTestClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
