package com.puchain.fep.web.tlq.queue.repository;

import com.puchain.fep.web.tlq.queue.domain.TlqQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for TLQ queue config. Minimal stub — full implementation in Task 3.
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface TlqQueueConfigRepository extends JpaRepository<TlqQueueConfig, String> {

    /**
     * 按节点 ID 判断是否存在关联队列。
     *
     * @param nodeId 节点 ID
     * @return 是否存在关联队列
     */
    boolean existsByNodeId(String nodeId);
}
