package com.puchain.fep.web.callback.repository;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 取最早的 ≤50 条指定状态条目（runner 批轮询）。
     *
     * @param status {@code CallbackQueueStatus} 常量值
     * @return 按 createTime 升序，≤50 条
     */
    List<CallbackQueueEntity> findTop50ByStatusOrderByCreateTimeAsc(String status);
}
