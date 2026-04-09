package com.puchain.fep.web.tlq.queue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * TLQ 队列配置 Entity stub。完整实现在 Task 3。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_tlq_queue_config")
public class TlqQueueConfig {

    /** 队列唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "queue_id", length = 32)
    private String queueId;

    /** 所属节点 ID。 */
    @Column(name = "node_id", length = 32)
    private String nodeId;

    /** 无参构造方法（JPA 要求）。 */
    public TlqQueueConfig() { /* for JPA */ }

    /**
     * 获取队列 ID。
     *
     * @return 队列 ID
     */
    public String getQueueId() {
        return queueId;
    }

    /**
     * 设置队列 ID。
     *
     * @param queueId 队列 ID
     */
    public void setQueueId(final String queueId) {
        this.queueId = queueId;
    }

    /**
     * 获取所属节点 ID。
     *
     * @return 节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 设置所属节点 ID。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }
}
