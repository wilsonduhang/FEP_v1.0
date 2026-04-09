package com.puchain.fep.web.tlq.queue.dto;

import com.puchain.fep.web.tlq.queue.domain.TlqChannelType;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * TLQ 队列配置创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §3.1.2 TLQ 队列管理（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqQueueConfigCreateRequest {

    /** 所属节点 ID。 */
    @NotBlank(message = "节点 ID 不能为空")
    private String nodeId;

    /** 队列名称。 */
    @NotBlank(message = "队列名称不能为空")
    @Size(min = 1, max = 100, message = "队列名称长度 1-100 字符")
    private String queueName;

    /** 通道类型。 */
    @NotNull(message = "通道类型不能为空")
    private TlqChannelType channelType;

    /** 队列类型。 */
    @NotNull(message = "队列类型不能为空")
    private TlqQueueType queueType;

    /** 队列描述（可选）。 */
    @Size(max = 500, message = "描述长度最大 500 字符")
    private String description;

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

    /**
     * 获取队列名称。
     *
     * @return 队列名称
     */
    public String getQueueName() {
        return queueName;
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
     * 获取通道类型。
     *
     * @return 通道类型枚举
     */
    public TlqChannelType getChannelType() {
        return channelType;
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
     * 获取队列类型。
     *
     * @return 队列类型枚举
     */
    public TlqQueueType getQueueType() {
        return queueType;
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
     * 获取队列描述（可为 null）。
     *
     * @return 描述信息
     */
    public String getDescription() {
        return description;
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
