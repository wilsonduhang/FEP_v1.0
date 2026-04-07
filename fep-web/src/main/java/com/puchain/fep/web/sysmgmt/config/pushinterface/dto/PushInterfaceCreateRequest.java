package com.puchain.fep.web.sysmgmt.config.pushinterface.dto;

import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.AuthType;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.PushMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 推送接口创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class PushInterfaceCreateRequest {

    @NotBlank(message = "接口名称不能为空")
    @Size(min = 1, max = 30, message = "接口名称长度必须在 1-30 字符之间")
    private String interfaceName;

    @NotBlank(message = "接口地址不能为空")
    @Pattern(regexp = "^https?://.*", message = "接口地址必须为合法 URL")
    @Size(max = 500, message = "接口地址最长 500 字符")
    private String interfaceUrl;

    @NotNull(message = "推送方式不能为空")
    private PushMethod pushMethod;

    private AuthType authType;

    private Integer timeoutSeconds;

    private Integer retryCount;

    private String businessTypeId;

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
     * 获取鉴权类型（可为 null，默认 NONE）。
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
     * 获取超时时间（秒，可为 null，默认 30）。
     *
     * @return 超时秒数
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置超时时间（秒）。
     *
     * @param timeoutSeconds 超时秒数
     */
    public void setTimeoutSeconds(final Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 获取重试次数（可为 null，默认 3）。
     *
     * @return 重试次数
     */
    public Integer getRetryCount() {
        return retryCount;
    }

    /**
     * 设置重试次数。
     *
     * @param retryCount 重试次数
     */
    public void setRetryCount(final Integer retryCount) {
        this.retryCount = retryCount;
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
}
