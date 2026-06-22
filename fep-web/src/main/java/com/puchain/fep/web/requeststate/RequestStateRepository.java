package com.puchain.fep.web.requeststate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * {@link RequestStateEntity} 仓储。S2 request-state tracking 子系统。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface RequestStateRepository extends JpaRepository<RequestStateEntity, String> {

    /**
     * 按 correlation key（8 位业务 transitionNo）查找请求状态行（inbound 结果归一匹配用）。
     *
     * @param correlationKey 8 位业务 transitionNo（归一后）
     * @return 命中行，未命中为 {@link Optional#empty()}
     */
    Optional<RequestStateEntity> findByCorrelationKey(String correlationKey);

    /**
     * 查询滞留请求：{@link RequestStateLifecycle#SENT} 且 {@code updatedAt} 早于阈值，且
     * <strong>非 correlation_blocked</strong>（结构性永等不到匹配的行被排除，避免 STUCK 计数被
     * 已知缺口污染，见 {@link BlockedMessageTypes}）。由 reaper 标记为
     * {@link RequestStateLifecycle#STUCK}。
     *
     * @param threshold 滞留阈值（now - TTL）；{@code updatedAt} 早于此即视为滞留
     * @return 滞留 SENT 非阻塞行，可能为空
     */
    @Query("""
        SELECT r FROM RequestStateEntity r
        WHERE r.lifecycleStatus = com.puchain.fep.web.requeststate.RequestStateLifecycle.SENT
          AND r.correlationBlocked = false
          AND r.updatedAt < :threshold
        """)
    List<RequestStateEntity> findStuck(@Param("threshold") Instant threshold);

    /**
     * 统计指定生命周期状态的请求行数（{@link RequestStateMetrics} 按 lifecycle 计数 gauge 用）。
     *
     * @param lifecycleStatus 生命周期状态
     * @return 该状态行数（≥0）
     */
    long countByLifecycleStatus(RequestStateLifecycle lifecycleStatus);

    /**
     * 统计 {@code correlation_blocked = true} 的请求行数（与 STUCK 计数区分——结构性永等不到匹配的行
     * 不计入 STUCK，见 {@link BlockedMessageTypes}）。
     *
     * <p>重构后（DEF-MC-1）仅作 {@link RequestStateMetrics} 等价测试的 oracle，生产 gauge 走
     * {@link #aggregateLifecycleAndBlockedCounts()} 单次聚合。</p>
     *
     * @return correlation_blocked 行数（≥0）
     */
    long countByCorrelationBlockedTrue();

    /**
     * 单次聚合统计 5 个 lifecycle 状态行数 + correlation_blocked 行数（{@link RequestStateMetrics}
     * gauge 用，把每次 Prometheus scrape 的 6 次 {@code COUNT} 往返压为 1 次，DEF-MC-1）。
     *
     * <p>镜像同模块 {@code SubSubmissionRecordRepository.aggregatePushStatusCounts()} 的单行
     * {@code SUM(CASE WHEN ...)} 范式：固定 6 列、<strong>无 {@code GROUP BY}</strong>，空表也恒返回
     * 单行全 {@code null}（调用方须 null→0）。<strong>列序固定</strong>：
     * {@code [CREATED, SENT, RESULT_RECEIVED, FAILED, STUCK, blocked]}。blocked 行同时计入其 lifecycle
     * 桶与 blocked 列（二者正交，逐字保持旧 {@code countByLifecycleStatus} 不含 blocked 过滤的语义）。</p>
     *
     * @return 单行 {@code Object[]}：{@code [createdCount, sentCount, resultReceivedCount,
     *         failedCount, stuckCount, blockedCount]}（外层 {@link List} 恒含 1 行；各元素空表时为 null）
     */
    @Query("SELECT "
            + "SUM(CASE WHEN r.lifecycleStatus = "
            + "com.puchain.fep.web.requeststate.RequestStateLifecycle.CREATED THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.lifecycleStatus = "
            + "com.puchain.fep.web.requeststate.RequestStateLifecycle.SENT THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.lifecycleStatus = "
            + "com.puchain.fep.web.requeststate.RequestStateLifecycle.RESULT_RECEIVED THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.lifecycleStatus = "
            + "com.puchain.fep.web.requeststate.RequestStateLifecycle.FAILED THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.lifecycleStatus = "
            + "com.puchain.fep.web.requeststate.RequestStateLifecycle.STUCK THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.correlationBlocked = true THEN 1L ELSE 0L END) "
            + "FROM RequestStateEntity r")
    List<Object[]> aggregateLifecycleAndBlockedCounts();
}
