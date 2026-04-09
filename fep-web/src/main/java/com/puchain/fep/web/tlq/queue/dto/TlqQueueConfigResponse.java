package com.puchain.fep.web.tlq.queue.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.tlq.queue.domain.TlqChannelType;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueConfig;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueType;

import java.time.LocalDateTime;

/**
 * TLQ 队列配置响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqQueueConfigResponse {

    /** 队列 ID。 */
    private String queueId;

    /** 所属节点 ID。 */
    private String nodeId;

    /** 队列名称。 */
    private String queueName;

    /** 通道类型。 */
    private TlqChannelType channelType;

    /** 队列类型。 */
    private TlqQueueType queueType;

    /** 队列状态。 */
    private EnableDisableStatus queueStatus;

    /** 队列描述。 */
    private String description;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 队列配置 Entity
     * @return 响应 DTO
     */
    public static TlqQueueConfigResponse fromEntity(final TlqQueueConfig entity) {
        TlqQueueConfigResponse resp = new TlqQueueConfigResponse();
        resp.queueId = entity.getQueueId();
        resp.nodeId = entity.getNodeId();
        resp.queueName = entity.getQueueName();
        resp.channelType = entity.getChannelType();
        resp.queueType = entity.getQueueType();
        resp.queueStatus = entity.getQueueStatus();
        resp.description = entity.getDescription();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取队列 ID。
     *
     * @return 队列 ID
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
}
