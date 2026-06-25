package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * P5 outbound consumer 持锁批量声领 Repository (P5 T2).
 *
 * <p>该 Repository 与 P4 已 ship 的
 * {@code com.puchain.fep.web.outbound.OutboundMessageQueueRepository} 并存：</p>
 * <ul>
 *   <li>P4 Repository 服务于 collector enqueue 写入侧（{@code save / existsByIdempotencyKey}）</li>
 *   <li>本 Repository 服务于 P5 consumer poll 读取侧（{@code claimBatch} 单一职责）</li>
 * </ul>
 *
 * <p>{@link #claimBatch(int)} 使用原生 SQL + {@code FOR UPDATE SKIP LOCKED}：</p>
 * <ul>
 *   <li>过滤条件：{@code status='PENDING'} 或 ({@code status='RETRY'} 且
 *       {@code next_retry_at <= CURRENT_TIMESTAMP}) — 跳过未到 backoff 窗口的重试行</li>
 *   <li>排序：{@code next_retry_at NULLS FIRST, queue_id ASC} — 让 PENDING (NULL) 优先，
 *       同等条件下按 queue_id 字典序确定（避免 H2/MySQL 默认实现差异）</li>
 *   <li>{@code SKIP LOCKED} 让多个 Consumer 实例并行 poll 时互不阻塞，落在已被锁住的行直接跳过</li>
 *   <li><b>不</b>使用 JPA {@code @Lock(PESSIMISTIC_WRITE)} 注解 — Hibernate 6.x 拒绝在原生
 *       查询上设置 lock mode（{@code "Illegal attempt to set lock mode for a native query"}），
 *       原生 SQL 中的 {@code FOR UPDATE} 已经向数据库下达了同等的锁意图</li>
 * </ul>
 *
 * <p>H2 ≥ 2.1 + MySQL 8.0+ 均支持 {@code FOR UPDATE SKIP LOCKED} 语法；本测试使用
 * {@code MODE=MySQL} 的 H2，与生产 MySQL 行为一致。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface OutboundQueueRepository extends JpaRepository<OutboundMessageQueueEntity, String> {

    /**
     * 批量声领 ≤ {@code batchSize} 条待发送的 queue_id。
     *
     * @param batchSize 单轮 poll 的最大批量上限（来自 {@link OutboundQueueProperties#batchSize()}）
     * @return 已被本事务持锁的 queue_id 列表，可能为空
     */
    @Query(value = """
        SELECT queue_id FROM outbound_message_queue
        WHERE status = 'PENDING'
           OR (status = 'RETRY' AND next_retry_at <= CURRENT_TIMESTAMP)
        ORDER BY next_retry_at NULLS FIRST, queue_id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> claimBatch(@Param("batchSize") int batchSize);

    /**
     * 统计当前积压条数：{@code PENDING} 或（{@code RETRY} 且
     * {@code next_retry_at<=CURRENT_TIMESTAMP}）。
     *
     * <p>供 {@code QueueBacklogMonitor} 周期采样积压告警（DEF-B9-3）。纯读不加锁
     * （无 {@code FOR UPDATE}），过滤条件与 {@link #claimBatch(int)} 一致（去 ORDER/LIMIT）。</p>
     *
     * @return 积压条数（≥0）
     */
    @Query(value = """
        SELECT COUNT(*) FROM outbound_message_queue
        WHERE status = 'PENDING'
           OR (status = 'RETRY' AND next_retry_at <= CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    long countBacklog();
}
