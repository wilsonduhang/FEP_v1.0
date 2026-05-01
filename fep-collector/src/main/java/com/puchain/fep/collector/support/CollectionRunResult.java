package com.puchain.fep.collector.support;

import java.util.Objects;

/**
 * 单次采集运行结果（不可变 record）。
 *
 * <p>由 {@code CollectorScheduler.runAdapter} 编排返回，作为：
 * <ul>
 *   <li>{@code triggerManually(adapterId)} 的同步返回值（手动触发链路）</li>
 *   <li>调度链路（cron）下用于 metrics / 日志聚合（不向外暴露）</li>
 * </ul>
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code runId}        — 32 位 hex 运行 ID（{@code IdGenerator.uuid32()}）；当
 *       状态为 {@link Status#SKIPPED}（未实际启动 run）时允许为 {@code null}</li>
 *   <li>{@code adapterId}    — 触发的适配器 ID</li>
 *   <li>{@code status}       — 终态枚举（{@link Status}）</li>
 *   <li>{@code assembled}    — 已组装记录数（含 dup-key 已视为成功的）</li>
 *   <li>{@code submitted}    — 已提交至 enqueue port 的记录数（含 dup-key）</li>
 *   <li>{@code errors}       — 失败记录数</li>
 *   <li>{@code errorMessage} — 首个错误的 message；无错误时为 {@code null}</li>
 * </ul>
 *
 * <p>compact 构造函数对 {@code adapterId} / {@code status} 做 null 校验。
 * {@code runId} / {@code errorMessage} 允许为 null（语义见上）。
 *
 * @param runId        运行 ID（SKIPPED 时允许 null）
 * @param adapterId    触发适配器 ID（非 null）
 * @param status       终态（非 null）
 * @param assembled    已组装数
 * @param submitted    已提交数
 * @param errors       失败数
 * @param errorMessage 首个错误消息（无错误时 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record CollectionRunResult(
        String runId,
        String adapterId,
        Status status,
        int assembled,
        int submitted,
        int errors,
        String errorMessage
) {

    /**
     * compact 构造函数 — null 校验必填字段。
     */
    public CollectionRunResult {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(status, "status");
    }

    /**
     * 构造 SKIPPED 结果（典型场景：分布式锁忙）。
     *
     * @param adapterId 适配器 ID（非 null）
     * @return SKIPPED 状态的结果（runId=null / 计数全 0 / 无 errorMessage）
     */
    public static CollectionRunResult skipped(final String adapterId) {
        return new CollectionRunResult(null, adapterId, Status.SKIPPED, 0, 0, 0, null);
    }

    /**
     * 采集运行终态。
     */
    public enum Status {

        /** 全部记录成功（含 dup-key 容忍计入成功）。 */
        SUCCESS,

        /** 部分记录失败但仍有成功提交。 */
        PARTIAL,

        /** 全部失败 / 编排级未捕获异常。 */
        FAILED,

        /** 未启动运行（典型：分布式锁忙）。 */
        SKIPPED,

        /** 拒绝触发（保留枚举位，目前由 {@code FepBusinessException} 抛出而非返回）。 */
        REJECTED
    }
}
