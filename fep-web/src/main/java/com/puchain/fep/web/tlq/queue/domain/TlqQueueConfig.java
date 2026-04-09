package com.puchain.fep.web.tlq.queue.domain;

import com.puchain.fep.common.domain.EnableDisableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TLQ 队列配置 Entity，映射 t_tlq_queue_config 表。
 *
 * <p>参见 PRD v1.3 §3.1.2 TLQ 队列命名规范与队列管理（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_tlq_queue_config")
@EntityListeners(AuditingEntityListener.class)
public class TlqQueueConfig {

    /** 队列唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "queue_id", length = 32)
    private String queueId;

    /** 所属节点 ID（外键关联 t_tlq_node）。 */
    @Column(name = "node_id", nullable = false, length = 32)
    private String nodeId;

    /** 队列名称（全局唯一）。 */
    @Column(name = "queue_name", nullable = false, length = 100)
    private String queueName;

    /** 通道类型（实时/批量）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private TlqChannelType channelType;

    /** 队列类型（LOCAL/REMOTE/DEST/SEND/DEAD）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false, length = 20)
    private TlqQueueType queueType;

    /** 队列状态（ENABLED / DISABLED）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", nullable = false, length = 20)
    private EnableDisableStatus queueStatus;

    /** 队列描述（可为 null）。 */
    @Column(name = "description", length = 500)
    private String description;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public TlqQueueConfig() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取队列唯一标识。
     *
     * @return 队列 ID (UUID 32位)
     */
    public String getQueueId() {
        return queueId;
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
     * 获取队列名称。
     *
     * @return 队列名称
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * 获取通道类型。
     *
     * @return 通道类型枚举
     */
    public TlqChannelType getChannelType() {
        return channelType;
    }

    /**
     * 获取队列类型。
     *
     * @return 队列类型枚举
     */
    public TlqQueueType getQueueType() {
        return queueType;
    }

    /**
     * 获取队列状态。
     *
     * @return 队列状态枚举
     */
    public EnableDisableStatus getQueueStatus() {
        return queueStatus;
    }

    /**
     * 获取队列描述（可为 null）。
     *
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置队列唯一标识。
     *
     * @param queueId 队列 ID
     */
    public void setQueueId(final String queueId) {
        this.queueId = queueId;
    }

    /**
     * 设置所属节点 ID。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 设置队列名称。
     *
     * @param queueName 队列名称
     */
    public void setQueueName(final String queueName) {
        this.queueName = queueName;
    }

    /**
     * 设置通道类型。
     *
     * @param channelType 通道类型枚举
     */
    public void setChannelType(final TlqChannelType channelType) {
        this.channelType = channelType;
    }

    /**
     * 设置队列类型。
     *
     * @param queueType 队列类型枚举
     */
    public void setQueueType(final TlqQueueType queueType) {
        this.queueType = queueType;
    }

    /**
     * 设置队列状态。
     *
     * @param queueStatus 队列状态枚举
     */
    public void setQueueStatus(final EnableDisableStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    /**
     * 设置队列描述。
     *
     * @param description 描述信息（可为 null）
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
