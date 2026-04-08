package com.puchain.fep.web.submission.outputinterface.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;

import java.time.LocalDateTime;

/**
 * 输出接口响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class OutputInterfaceResponse {

    /** 接口 ID。 */
    private String interfaceId;

    /** 接口名称。 */
    private String interfaceName;

    /** 接口地址。 */
    private String interfaceUrl;

    /** 关联业务类型 ID。 */
    private String businessTypeId;

    /** 鉴权类型。 */
    private InterfaceAuthType authType;

    /** 超时时间（秒）。 */
    private int timeoutSeconds;

    /** 重试次数。 */
    private int retryCount;

    /** 接口状态。 */
    private EnableDisableStatus interfaceStatus;

    /** 最近调用时间。 */
    private LocalDateTime lastCallTime;

    /** 调用统计。 */
    private long callCount;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 输出接口 Entity
     * @return 响应 DTO
     */
    public static OutputInterfaceResponse from(final SubOutputInterface entity) {
        OutputInterfaceResponse resp = new OutputInterfaceResponse();
        resp.interfaceId = entity.getInterfaceId();
        resp.interfaceName = entity.getInterfaceName();
        resp.interfaceUrl = entity.getInterfaceUrl();
        resp.businessTypeId = entity.getBusinessTypeId();
        resp.authType = entity.getAuthType();
        resp.timeoutSeconds = entity.getTimeoutSeconds();
        resp.retryCount = entity.getRetryCount();
        resp.interfaceStatus = entity.getInterfaceStatus();
        resp.lastCallTime = entity.getLastCallTime();
        resp.callCount = entity.getCallCount();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
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
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID（可为 null）
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
     * 获取最近调用时间。
     *
     * @return 最近调用时间（可为 null）
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
}
