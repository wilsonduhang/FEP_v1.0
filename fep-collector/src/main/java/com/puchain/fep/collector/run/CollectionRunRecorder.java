package com.puchain.fep.collector.run;

import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.collector.support.TriggerType;

import java.time.Instant;

/**
 * 采集运行记账 Port（forward-declared，由 T8 实现 {@code JdbcCollectionRunRecorder} +
 * V19 SQL 迁移）。
 *
 * <p>负责把单次 {@code CollectorScheduler.runAdapter} 的生命周期事件（启动 / 完成）
 * 持久化到 {@code collection_run} 表，便于运维侧查询历史运行（T6b
 * {@code CollectionRunController}）与失败重试。
 *
 * <p><b>实现约定：</b>
 * <ul>
 *   <li>{@link #start} 在调度器拿到分布式锁后立即调用，写 {@code RUNNING} 行</li>
 *   <li>{@link #complete} 在 finally 块前调用，更新终态 + 计数 + 错误消息</li>
 *   <li>持久化失败必须抛 {@code FepBusinessException(COLLECT_PERSIST_FAILURE)} —
 *       调度器据此返回 FAILED 状态而非吞异常</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CollectionRunRecorder {

    /**
     * 在 run 启动时持久化 RUNNING 行。
     *
     * @param runId       运行 ID（32 hex，非 null）
     * @param adapterId   适配器 ID（非 null）
     * @param triggerType 触发方式（非 null）
     * @param startedAt   启动时刻（非 null）
     */
    void start(String runId, String adapterId, TriggerType triggerType, Instant startedAt);

    /**
     * 在 run 完成时更新终态 + 计数。
     *
     * @param runId        运行 ID（与 {@link #start} 同一次）
     * @param status       终态（{@link CollectionRunResult.Status}，非 null）
     * @param collected    {@code adapter.collect} 返回的原始记录条数（含被跳过的脏数据）；
     *                     T10 Simplify Q-2 fix — 用于填充 V23 {@code collected_count} 列，
     *                     使管理 UI 能区分"采集到的总量"与"已组装/入队成功量"
     * @param assembled    已组装数
     * @param submitted    已提交数
     * @param errors       失败数
     * @param errorMessage 首个错误消息（无错误时传 null）
     * @param completedAt  完成时刻（非 null）
     */
    void complete(String runId,
                  CollectionRunResult.Status status,
                  int collected,
                  int assembled,
                  int submitted,
                  int errors,
                  String errorMessage,
                  Instant completedAt);
}
