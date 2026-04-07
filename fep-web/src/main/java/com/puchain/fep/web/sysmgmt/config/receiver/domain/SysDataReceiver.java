package com.puchain.fep.web.sysmgmt.config.receiver.domain;

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
 * 数据接收方 Entity，映射 t_sys_data_receiver 表。
 *
 * <p>参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_data_receiver")
@EntityListeners(AuditingEntityListener.class)
public class SysDataReceiver {

    @Id
    @Column(name = "receiver_id", length = 32)
    private String receiverId;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_method", nullable = false, length = 20)
    private ReceiverMethod receiverMethod;

    @Column(name = "receiver_address", length = 500)
    private String receiverAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_status", nullable = false, length = 20)
    private ReceiverStatus receiverStatus;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysDataReceiver() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取接收方唯一标识。
     *
     * @return 接收方 ID (UUID 32位)
     */
    public String getReceiverId() {
        return receiverId;
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
     * 获取接收方式。
     *
     * @return 接收方式枚举
     */
    public ReceiverMethod getReceiverMethod() {
        return receiverMethod;
    }

    /**
     * 获取接收地址。
     *
     * @return 接收地址（可为 null）
     */
    public String getReceiverAddress() {
        return receiverAddress;
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
     * 设置接收方唯一标识。
     *
     * @param receiverId 接收方 ID
     */
    public void setReceiverId(final String receiverId) {
        this.receiverId = receiverId;
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
     * 设置接收方式。
     *
     * @param receiverMethod 接收方式枚举
     */
    public void setReceiverMethod(final ReceiverMethod receiverMethod) {
        this.receiverMethod = receiverMethod;
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
     * 设置接收方状态。
     *
     * @param receiverStatus 状态枚举
     */
    public void setReceiverStatus(final ReceiverStatus receiverStatus) {
        this.receiverStatus = receiverStatus;
    }
}
