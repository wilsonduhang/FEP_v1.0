package com.puchain.fep.collector.support;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 单次采集运行上下文（不可变值对象，Java record）。
 *
 * <p>由调度器在每次触发时构造，传递给
 * {@link CollectorAdapter#collect(CollectionRunContext)} 与
 * {@link CollectorAdapter#acknowledge(CollectionRunContext, java.util.List)}。
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code runId} — 32 字符 UUID hex，本次运行唯一标识（用于日志 / 审计追溯）</li>
 *   <li>{@code adapterId} — 与 {@link CollectorAdapter#getId()} 一致</li>
 *   <li>{@code triggerType} — 触发方式（SCHEDULED / MANUAL）</li>
 *   <li>{@code previousWatermark} — 上次成功推进的水位（首次运行为 {@link Optional#empty}）</li>
 *   <li>{@code startedAt} — 本次运行启动时刻（UTC Instant）</li>
 *   <li>{@code batchSize} — 本次运行单批次条数上限（来自 {@code fep.collector.batch-size}）</li>
 * </ul>
 *
 * <p>compact 构造函数对所有引用字段执行 {@link Objects#requireNonNull} 校验。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public record CollectionRunContext(
        String runId,
        String adapterId,
        TriggerType triggerType,
        Optional<String> previousWatermark,
        Instant startedAt,
        int batchSize
) {

    /**
     * compact 构造函数 — null 校验所有引用字段。
     */
    public CollectionRunContext {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(triggerType, "triggerType");
        Objects.requireNonNull(previousWatermark, "previousWatermark");
        Objects.requireNonNull(startedAt, "startedAt");
    }
}
