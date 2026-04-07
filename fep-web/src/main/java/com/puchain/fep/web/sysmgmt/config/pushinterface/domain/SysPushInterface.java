package com.puchain.fep.web.sysmgmt.config.pushinterface.domain;

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
 * 推送接口 Entity，映射 t_sys_push_interface 表。
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_push_interface")
@EntityListeners(AuditingEntityListener.class)
public class SysPushInterface {

    @Id
    @Column(name = "interface_id", length = 32)
    private String interfaceId;

    @Column(name = "interface_name", nullable = false, length = 100)
    private String interfaceName;

    @Column(name = "interface_url", nullable = false, length = 500)
    private String interfaceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "push_method", nullable = false, length = 20)
    private PushMethod pushMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "business_type_id", length = 32)
    private String businessTypeId;

    @Column(name = "last_push_time")
    private LocalDateTime lastPushTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "interface_status", nullable = false, length = 20)
    private InterfaceStatus interfaceStatus;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysPushInterface() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取推送接口唯一标识。
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
     * 获取接口 URL。
     *
     * @return 接口 URL
     */
    public String getInterfaceUrl() {
        return interfaceUrl;
    }

    /**
     * 获取推送方式。
     *
     * @return 推送方式枚举
     */
    public PushMethod getPushMethod() {
        return pushMethod;
    }

    /**
     * 获取鉴权类型。
     *
     * @return 鉴权类型枚举
     */
    public AuthType getAuthType() {
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
     * 获取关联业务类型 ID（可为 null）。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取最近推送时间（可为 null）。
     *
     * @return 最近推送时间
     */
    public LocalDateTime getLastPushTime() {
        return lastPushTime;
    }

    /**
     * 获取接口状态。
     *
     * @return 接口状态枚举
     */
    public InterfaceStatus getInterfaceStatus() {
        return interfaceStatus;
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
     * 设置推送接口唯一标识。
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
     * 设置接口 URL。
     *
     * @param interfaceUrl 接口 URL
     */
    public void setInterfaceUrl(final String interfaceUrl) {
        this.interfaceUrl = interfaceUrl;
    }

    /**
     * 设置推送方式。
     *
     * @param pushMethod 推送方式枚举
     */
    public void setPushMethod(final PushMethod pushMethod) {
        this.pushMethod = pushMethod;
    }

    /**
     * 设置鉴权类型。
     *
     * @param authType 鉴权类型枚举
     */
    public void setAuthType(final AuthType authType) {
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
     * 设置关联业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID（可为 null）
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 设置最近推送时间。
     *
     * @param lastPushTime 最近推送时间（可为 null）
     */
    public void setLastPushTime(final LocalDateTime lastPushTime) {
        this.lastPushTime = lastPushTime;
    }

    /**
     * 设置接口状态。
     *
     * @param interfaceStatus 接口状态枚举
     */
    public void setInterfaceStatus(final InterfaceStatus interfaceStatus) {
        this.interfaceStatus = interfaceStatus;
    }
}
