package com.puchain.fep.web.tlq.queue.repository;

import com.puchain.fep.web.tlq.queue.domain.TlqQueueConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TLQ 队列配置 Repository。
 *
 * <p>参见 PRD v1.3 §3.1.2 TLQ 队列管理（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface TlqQueueConfigRepository extends JpaRepository<TlqQueueConfig, String> {

    /**
     * 按队列名称判断是否存在。
     *
     * @param queueName 队列名称
     * @return 是否存在
     */
    boolean existsByQueueName(String queueName);

    /**
     * 按节点 ID 判断是否存在关联队列。
     *
     * @param nodeId 节点 ID
     * @return 是否存在关联队列
     */
    boolean existsByNodeId(String nodeId);

    /**
     * 按节点 ID 查询队列列表，按通道类型升序、队列类型升序排列。
     *
     * @param nodeId 节点 ID
     * @return 队列配置列表
     */
    List<TlqQueueConfig> findByNodeIdOrderByChannelTypeAscQueueTypeAsc(String nodeId);
}
