package com.puchain.fep.web.sysmgmt.config.receiver.dto;

import com.puchain.fep.web.sysmgmt.config.receiver.domain.ReceiverMethod;
import com.puchain.fep.web.sysmgmt.config.receiver.domain.ReceiverStatus;
import com.puchain.fep.web.sysmgmt.config.receiver.domain.SysDataReceiver;

import java.time.LocalDateTime;

/**
 * 数据接收方响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DataReceiverResponse {

    private String receiverId;
    private String receiverName;
    private ReceiverMethod receiverMethod;
    private String receiverAddress;
    private ReceiverStatus receiverStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysDataReceiver Entity 构建响应 DTO。
     *
     * @param entity 数据接收方 Entity
     * @return 响应 DTO
     */
    public static DataReceiverResponse from(final SysDataReceiver entity) {
        DataReceiverResponse resp = new DataReceiverResponse();
        resp.setReceiverId(entity.getReceiverId());
        resp.setReceiverName(entity.getReceiverName());
        resp.setReceiverMethod(entity.getReceiverMethod());
        resp.setReceiverAddress(entity.getReceiverAddress());
        resp.setReceiverStatus(entity.getReceiverStatus());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取接收方 ID。
     *
     * @return 接收方 ID
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * 设置接收方 ID。
     *
     * @param receiverId 接收方 ID
     */
    public void setReceiverId(final String receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * 获取接收方名称。
     *
     * @return 接收方名称
     */
    public String getReceiverName() {
        return receiverName;
    }

    /**
     * 设置接收方名称。
     *
     * @param receiverName 接收方名称
     */
    public void setReceiverName(final String receiverName) {
        this.receiverName = receiverName;
    }

    /**
     * 获取接收方式。
     *
     * @return 接收方式枚举
     */
    public ReceiverMethod getReceiverMethod() {
        return receiverMethod;
    }

    /**
     * 设置接收方式。
     *
     * @param receiverMethod 接收方式枚举
     */
    public void setReceiverMethod(final ReceiverMethod receiverMethod) {
        this.receiverMethod = receiverMethod;
    }

    /**
     * 获取接收地址。
     *
     * @return 接收地址
     */
    public String getReceiverAddress() {
        return receiverAddress;
    }

    /**
     * 设置接收地址。
     *
     * @param receiverAddress 接收地址
     */
    public void setReceiverAddress(final String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    /**
     * 获取接收方状态。
     *
     * @return 状态枚举
     */
    public ReceiverStatus getReceiverStatus() {
        return receiverStatus;
    }

    /**
     * 设置接收方状态。
     *
     * @param receiverStatus 状态枚举
     */
    public void setReceiverStatus(final ReceiverStatus receiverStatus) {
        this.receiverStatus = receiverStatus;
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
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
