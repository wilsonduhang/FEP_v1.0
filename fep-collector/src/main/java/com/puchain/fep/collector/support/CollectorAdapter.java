package com.puchain.fep.collector.support;

import java.util.List;

/**
 * 数据采集适配器统一抽象。
 *
 * <p>每个 {@code CollectorAdapter} 实现对应一个外部数据源（JDBC 表 / 文件目录 /
 * MQ topic / ESB 接口）。调度器在每次触发时按以下流程驱动：
 *
 * <ol>
 *   <li>构造 {@code CollectionRunContext}（含 runId / previousWatermark / batchSize）</li>
 *   <li>调用 {@link #collect(CollectionRunContext)} 取回本次批次记录</li>
 *   <li>下游入队成功后调用 {@link #acknowledge(CollectionRunContext, List)}
 *       推进水位（at-least-once 语义）</li>
 * </ol>
 *
 * <p><b>实现约定：</b>
 * <ul>
 *   <li>{@link #getId()} 必须与 {@code fep.collector.adapters[*].id} 一致</li>
 *   <li>{@link #collect(CollectionRunContext)} 必须返回不可变 List（推荐 {@code List.copyOf}）</li>
 *   <li>{@link #acknowledge(CollectionRunContext, List)} 失败必须抛异常（不可静默吞）—
 *       调度器据此决定是否推进水位</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CollectorAdapter {

    /**
     * 适配器逻辑 ID。
     *
     * @return 适配器 ID（非 null / 非空），与 {@code fep.collector.adapters[*].id} 一致
     */
    String getId();

    /**
     * 适配器类型。
     *
     * @return 适配器类型（非 null），决定 {@code AdapterFactory} 装配策略
     */
    AdapterType getType();

    /**
     * 采集本次批次记录。
     *
     * <p>实现必须基于 {@code context.previousWatermark()} 增量取数，
     * 单次返回不超过 {@code context.batchSize()} 条。
     *
     * @param context 本次运行上下文（非 null）
     * @return 采集到的记录列表（非 null，可能为空 List；推荐返回不可变 List）
     */
    List<CollectionRecord> collect(CollectionRunContext context);

    /**
     * 确认本批次记录已成功入队 / 处理，推进水位。
     *
     * <p>调度器仅在下游入队成功后才调用本方法。失败必须抛异常以阻止水位推进。
     *
     * @param context 与 {@link #collect(CollectionRunContext)} 同一次运行的上下文（非 null）
     * @param records 本批次实际入队成功的记录列表（非 null，可为空表示本批无新增）
     */
    void acknowledge(CollectionRunContext context, List<CollectionRecord> records);
}
