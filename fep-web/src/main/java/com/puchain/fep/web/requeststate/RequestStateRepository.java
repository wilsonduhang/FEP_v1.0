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
}
