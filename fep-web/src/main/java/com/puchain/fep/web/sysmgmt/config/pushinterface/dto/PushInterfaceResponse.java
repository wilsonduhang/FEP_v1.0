package com.puchain.fep.web.sysmgmt.config.pushinterface.dto;

import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.AuthType;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.InterfaceStatus;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.PushMethod;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.SysPushInterface;

import java.time.LocalDateTime;

/**
 * 推送接口响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class PushInterfaceResponse {

    private String interfaceId;
    private String interfaceName;
    private String interfaceUrl;
    private PushMethod pushMethod;
    private AuthType authType;
    private int timeoutSeconds;
    private int retryCount;
    private String businessTypeId;
    private String businessTypeName;
    private LocalDateTime lastPushTime;
    private InterfaceStatus interfaceStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysPushInterface Entity 构建响应 DTO（不含业务类型名称）。
     *
     * @param entity 推送接口 Entity
     * @return 响应 DTO
     */
    public static PushInterfaceResponse from(final SysPushInterface entity) {
        return from(entity, null);
    }

    /**
     * 从 SysPushInterface Entity 构建响应 DTO（含业务类型名称）。
     *
     * @param entity           推送接口 Entity
     * @param businessTypeName 关联业务类型名称（可为 null）
     * @return 响应 DTO
     */
    public static PushInterfaceResponse from(final SysPushInterface entity,
                                              final String businessTypeName) {
        PushInterfaceResponse resp = new PushInterfaceResponse();
        resp.setInterfaceId(entity.getInterfaceId());
        resp.setInterfaceName(entity.getInterfaceName());
        resp.setInterfaceUrl(entity.getInterfaceUrl());
        resp.setPushMethod(entity.getPushMethod());
        resp.setAuthType(entity.getAuthType());
        resp.setTimeoutSeconds(entity.getTimeoutSeconds());
        resp.setRetryCount(entity.getRetryCount());
        resp.setBusinessTypeId(entity.getBusinessTypeId());
        resp.setBusinessTypeName(businessTypeName);
        resp.setLastPushTime(entity.getLastPushTime());
        resp.setInterfaceStatus(entity.getInterfaceStatus());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取接口 ID。
     *
     * @return 接口 ID
     */
    public String getInterfaceId() {
        return interfaceId;
    }

    /**
     * 设置接口 ID。
     *
     * @param interfaceId 接口 ID
     */
    public void setInterfaceId(final String interfaceId) {
        this.interfaceId = interfaceId;
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
     * 设置接口名称。
     *
     * @param interfaceName 接口名称
     */
    public void setInterfaceName(final String interfaceName) {
        this.interfaceName = interfaceName;
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
     * 设置接口 URL。
     *
     * @param interfaceUrl 接口 URL
     */
    public void setInterfaceUrl(final String interfaceUrl) {
        this.interfaceUrl = interfaceUrl;
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
     * 设置推送方式。
     *
     * @param pushMethod 推送方式枚举
     */
    public void setPushMethod(final PushMethod pushMethod) {
        this.pushMethod = pushMethod;
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
     * 设置鉴权类型。
     *
     * @param authType 鉴权类型枚举
     */
    public void setAuthType(final AuthType authType) {
        this.authType = authType;
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
     * 设置超时时间（秒）。
     *
     * @param timeoutSeconds 超时秒数
     */
    public void setTimeoutSeconds(final int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
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
     * 设置重试次数。
     *
     * @param retryCount 重试次数
     */
    public void setRetryCount(final int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID（可为 null）
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 设置关联业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 获取关联业务类型名称。
     *
     * @return 业务类型名称（可为 null）
     */
    public String getBusinessTypeName() {
        return businessTypeName;
    }

    /**
     * 设置关联业务类型名称。
     *
     * @param businessTypeName 业务类型名称
     */
    public void setBusinessTypeName(final String businessTypeName) {
        this.businessTypeName = businessTypeName;
    }

    /**
     * 获取最近推送时间。
     *
     * @return 最近推送时间（可为 null）
     */
    public LocalDateTime getLastPushTime() {
        return lastPushTime;
    }

    /**
     * 设置最近推送时间。
     *
     * @param lastPushTime 最近推送时间
     */
    public void setLastPushTime(final LocalDateTime lastPushTime) {
        this.lastPushTime = lastPushTime;
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
     * 设置接口状态。
     *
     * @param interfaceStatus 接口状态枚举
     */
    public void setInterfaceStatus(final InterfaceStatus interfaceStatus) {
        this.interfaceStatus = interfaceStatus;
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
