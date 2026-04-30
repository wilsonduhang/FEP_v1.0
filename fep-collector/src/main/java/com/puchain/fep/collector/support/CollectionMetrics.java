package com.puchain.fep.collector.support;

import java.util.concurrent.atomic.LongAdder;

/**
 * 采集层运行指标聚合器（基于 {@link LongAdder}，高并发友好）。
 *
 * <p>5 维计数：
 * <ul>
 *   <li>{@code collected} — 已采集</li>
 *   <li>{@code assembled} — 已组装</li>
 *   <li>{@code submitted} — 已提交</li>
 *   <li>{@code failed}    — 失败</li>
 *   <li>{@code skipped}   — 跳过</li>
 * </ul>
 *
 * <p><b>选择 LongAdder 的理由：</b>{@link LongAdder} 在高并发场景下吞吐显著优于
 * {@link java.util.concurrent.atomic.AtomicLong}（无 CAS 自旋热点，分段累加），
 * 适合采集层"每条记录 inc 1"的高频写场景。
 *
 * <p><b>未声明为 Spring Bean</b>：保持工具类身份，由调用方按需 new 出来注入到
 * {@code CollectionRunContext} 或 adapter 实现，避免单例 bean 在多 adapter 共享下混淆指标归属。
 *
 * <p><b>负 delta 允许：</b>{@link LongAdder#add(long)} 接受任意 long，本类不主动校验。
 * 业务上用于补偿 / 回退场景（例如发现重复采集后回退 collected）。
 *
 * <p><b>线程安全：</b>所有 incXxx 与 snapshot 均线程安全。snapshot 是
 * point-in-time 快照，无需额外锁。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class CollectionMetrics {

    private final LongAdder collected = new LongAdder();
    private final LongAdder assembled = new LongAdder();
    private final LongAdder submitted = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder skipped = new LongAdder();

    /**
     * 累加已采集记录数。
     *
     * @param delta 增量（允许负值，用于补偿 / 回退）
     */
    public void incCollected(final long delta) {
        collected.add(delta);
    }

    /**
     * 累加已组装报文数。
     *
     * @param delta 增量（允许负值）
     */
    public void incAssembled(final long delta) {
        assembled.add(delta);
    }

    /**
     * 累加已提交报文数。
     *
     * @param delta 增量（允许负值）
     */
    public void incSubmitted(final long delta) {
        submitted.add(delta);
    }

    /**
     * 累加失败记录数。
     *
     * @param delta 增量（允许负值）
     */
    public void incFailed(final long delta) {
        failed.add(delta);
    }

    /**
     * 累加跳过记录数。
     *
     * @param delta 增量（允许负值）
     */
    public void incSkipped(final long delta) {
        skipped.add(delta);
    }

    /**
     * 拍取当前各 LongAdder 累计值的不可变快照。
     *
     * <p>注意 {@link LongAdder#sum()} 在并发写入下不是原子读 —— 5 个字段分别 sum，
     * 跨字段一致性不保证（如 collected 已自增但 assembled 尚未读到）。这对监控埋点
     * 场景（最终一致性即可）是可接受的。
     *
     * @return 当前累计值的不可变 record（非 null）
     */
    public CollectionMetricsSnapshot snapshot() {
        return new CollectionMetricsSnapshot(
                collected.sum(),
                assembled.sum(),
                submitted.sum(),
                failed.sum(),
                skipped.sum()
        );
    }
}
