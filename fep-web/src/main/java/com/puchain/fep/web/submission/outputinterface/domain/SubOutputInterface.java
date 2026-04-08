package com.puchain.fep.web.submission.outputinterface.domain;

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
 * 输出接口 Entity，映射 t_sub_output_interface 表。
 *
 * <p>参见 PRD v1.3 §5.5.2 输出接口管理（FR-WEB-SUB-OUT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sub_output_interface")
@EntityListeners(AuditingEntityListener.class)
public class SubOutputInterface {

    /** 接口唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "interface_id", length = 32)
    private String interfaceId;

    /** 接口名称。 */
    @Column(name = "interface_name", nullable = false, length = 100)
    private String interfaceName;

    /** 接口地址（合法 URL）。 */
    @Column(name = "interface_url", nullable = false, length = 500)
    private String interfaceUrl;

    /** 关联业务类型 ID（可为 null）。 */
    @Column(name = "business_type_id", length = 32)
    private String businessTypeId;

    /** 鉴权类型。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private InterfaceAuthType authType;

    /** 超时时间（秒）。 */
    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    /** 重试次数。 */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** 接口状态（ENABLED / DISABLED）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "interface_status", nullable = false, length = 20)
    private EnableDisableStatus interfaceStatus;

    /** 最近调用时间。 */
    @Column(name = "last_call_time")
    private LocalDateTime lastCallTime;

    /** 调用统计。 */
    @Column(name = "call_count", nullable = false)
    private long callCount;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public SubOutputInterface() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取接口唯一标识。
     *
     * @return 接口 ID (UUID 32位)
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * 获取接口名称。
     *
     * @return 接口名称
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * 获取接口地址。
     *
     * @return 接口 URL
     */
    public String getInterfaceUrl() {
        return interfaceUrl;
    }

    /**
     * 获取关联业务类型 ID（可为 null）。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取鉴权类型。
     *
     * @return 鉴权类型枚举
     */
    public InterfaceAuthType getAuthType() {
        return authType;
    }

    /**
     * 获取超时时间（秒）。
     *
     * @return 超时秒数
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 获取重试次数。
     *
     * @return 重试次数
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * 获取接口状态。
     *
     * @return 接口状态枚举
     */
    public EnableDisableStatus getInterfaceStatus() {
        return interfaceStatus;
    }

    /**
     * 获取最近调用时间（可为 null）。
     *
     * @return 最近调用时间
     */
    public LocalDateTime getLastCallTime() {
        return lastCallTime;
    }

    /**
     * 获取调用统计。
     *
     * @return 调用次数
     */
    public long getCallCount() {
        return callCount;
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
     * 设置接口唯一标识。
     *
     * @param interfaceId 接口 ID
     */
    public void setInterfaceId(final String interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * 设置接口名称。
     *
     * @param interfaceName 接口名称
     */
    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * 设置接口地址。
     *
     * @param interfaceUrl 接口 URL
     */
    public void setInterfaceUrl(final String interfaceUrl) {
        this.interfaceUrl = interfaceUrl;
    }

    /**
     * 设置关联业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID（可为 null）
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 设置鉴权类型。
     *
     * @param authType 鉴权类型枚举
     */
    public void setAuthType(final InterfaceAuthType authType) {
        this.authType = authType;
    }

    /**
     * 设置超时时间（秒）。
     *
     * @param timeoutSeconds 超时秒数
     */
    public void setTimeoutSeconds(final int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 设置重试次数。
     *
     * @param retryCount 重试次数
     */
    public void setRetryCount(final int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 设置接口状态。
     *
     * @param interfaceStatus 接口状态枚举
     */
    public void setInterfaceStatus(final EnableDisableStatus interfaceStatus) {
        this.interfaceStatus = interfaceStatus;
    }

    /**
     * 设置最近调用时间。
     *
     * @param lastCallTime 最近调用时间（可为 null）
     */
    public void setLastCallTime(final LocalDateTime lastCallTime) {
        this.lastCallTime = lastCallTime;
    }

    /**
     * 设置调用统计。
     *
     * @param callCount 调用次数
     */
    public void setCallCount(final long callCount) {
        this.callCount = callCount;
    }
}
