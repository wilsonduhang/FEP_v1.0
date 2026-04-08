package com.puchain.fep.web.submission.outputinterface.dto;

import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 输出接口创建/编辑请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.5.2 输出接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class OutputInterfaceCreateRequest {

    /** 默认超时时间（秒）。 */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /** 默认重试次数。 */
    private static final int DEFAULT_RETRY_COUNT = 3;

    /** 接口名称。 */
    @NotBlank(message = "接口名称不能为空")
    @Size(min = 1, max = 30, message = "接口名称长度 1-30 字符")
    private String interfaceName;

    /** 接口地址（合法 URL）。 */
    @NotBlank(message = "接口地址不能为空")
    @URL(message = "接口地址必须是合法 URL")
    private String interfaceUrl;

    /** 关联业务类型 ID（可为 null）。 */
    private String businessTypeId;

    /** 鉴权类型。 */
    @NotNull(message = "鉴权类型不能为空")
    private InterfaceAuthType authType;

    /** 超时时间（秒），默认 30。 */
    @Min(value = 1, message = "超时时间最小 1 秒")
    @Max(value = 300, message = "超时时间最大 300 秒")
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    /** 重试次数，默认 3。 */
    @Min(value = 0, message = "重试次数最小 0 次")
    @Max(value = 10, message = "重试次数最大 10 次")
    private int retryCount = DEFAULT_RETRY_COUNT;

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
     * 获取接口地址。
     *
     * @return 接口 URL
     */
    public String getInterfaceUrl() {
        return interfaceUrl;
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
     * 获取关联业务类型 ID（可为 null）。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
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
     * 获取鉴权类型。
     *
     * @return 鉴权类型枚举
     */
    public InterfaceAuthType getAuthType() {
        return authType;
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
}
