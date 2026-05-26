package com.puchain.fep.web.callback.repository;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link CallbackQueueEntity} 仓储。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CallbackQueueRepository extends JpaRepository<CallbackQueueEntity, String> {

    /**
     * @param idempotencyKey 幂等键
     * @return 是否已存在（入队前幂等前置）
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * 批量声领 ≤ {@code batchSize} 条待发送/到期重试的 queue_id（多实例安全）。
     *
     * <p>过滤 {@code status='PENDING'} 或（{@code status='RETRY'} 且
     * {@code next_retry_at<=CURRENT_TIMESTAMP}）；{@code FOR UPDATE SKIP LOCKED}
     * 让多实例并行声领互不阻塞（镜像 {@code OutboundQueueRepository.claimBatch}）。</p>
     *
     * @param batchSize 单轮声领上限（{@code CallbackQueueProperties.batchSize}）
     * @return 已持锁的 queue_id 列表，可能为空
     */
    @Query(value = """
        SELECT queue_id FROM callback_queue
        WHERE status = 'PENDING'
           OR (status = 'RETRY' AND next_retry_at <= CURRENT_TIMESTAMP)
        ORDER BY next_retry_at NULLS FIRST, queue_id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> claimBatch(@Param("batchSize") int batchSize);
}
