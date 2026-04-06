package com.puchain.fep.web.sysmgmt.log.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 操作日志 Entity，映射 t_sys_operation_log 表。
 *
 * <p>记录用户对系统各功能模块的增删改查操作，用于安全审计与运维追踪。</p>
 * <p>参见 PRD v1.3 §5.10.6 日志管理 / §8.3 操作审计日志全覆盖。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_operation_log")
public class SysOperationLog {

    @Id
    @Column(name = "log_id", length = 32)
    private String logId;

    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "user_account", length = 50)
    private String userAccount;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 50)
    private OperationType operation;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "request_url", nullable = false, length = 500)
    private String requestUrl;

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysOperationLog() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取日志唯一标识。
     *
     * @return 日志 ID (UUID 32位)
     */
    public String getLogId() {
        return logId;
    }

    /**
     * 获取操作人用户 ID。
     *
     * @return 用户 ID，未认证时为 null
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 获取操作人账号快照。
     *
     * @return 操作人账号，未认证时为 null
     */
    public String getUserAccount() {
        return userAccount;
    }

    /**
     * 获取功能模块名称。
     *
     * @return 模块名称
     */
    public String getModule() {
        return module;
    }

    /**
     * 获取操作类型。
     *
     * @return 操作类型枚举
     */
    public OperationType getOperation() {
        return operation;
    }

    /**
     * 获取操作描述。
     *
     * @return 操作描述，可能为 null
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取 HTTP 方法。
     *
     * @return HTTP 方法（GET/POST/PUT/DELETE 等）
     */
    public String getMethod() {
        return method;
    }

    /**
     * 获取请求 URL。
     *
     * @return 请求路径
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * 获取请求参数（已截断至 2000 字符）。
     *
     * @return 请求参数字符串，可能为 null
     */
    public String getRequestParams() {
        return requestParams;
    }

    /**
     * 获取 HTTP 响应状态码。
     *
     * @return HTTP 状态码（200/400/500 等）
     */
    public Integer getResponseStatus() {
        return responseStatus;
    }

    /**
     * 获取客户端 IP 地址。
     *
     * @return IPv4 或 IPv6 地址字符串
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * 获取请求耗时。
     *
     * @return 耗时（毫秒）
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * 获取操作时间。
     *
     * @return 操作时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    // ===== Setters =====

    /**
     * 设置日志唯一标识。
     *
     * @param logId 日志 ID (UUID 32位)
     */
    public void setLogId(final String logId) {
        this.logId = logId;
    }

    /**
     * 设置操作人用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * 设置操作人账号快照。
     *
     * @param userAccount 操作人账号
     */
    public void setUserAccount(final String userAccount) {
        this.userAccount = userAccount;
    }

    /**
     * 设置功能模块名称。
     *
     * @param module 模块名称
     */
    public void setModule(final String module) {
        this.module = module;
    }

    /**
     * 设置操作类型。
     *
     * @param operation 操作类型枚举
     */
    public void setOperation(final OperationType operation) {
        this.operation = operation;
    }

    /**
     * 设置操作描述。
     *
     * @param description 操作描述
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 设置 HTTP 方法。
     *
     * @param method HTTP 方法
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * 设置请求 URL。
     *
     * @param requestUrl 请求路径
     */
    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * 设置请求参数（应已截断至 2000 字符）。
     *
     * @param requestParams 请求参数字符串
     */
    public void setRequestParams(final String requestParams) {
        this.requestParams = requestParams;
    }

    /**
     * 设置 HTTP 响应状态码。
     *
     * @param responseStatus HTTP 状态码
     */
    public void setResponseStatus(final Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * 设置客户端 IP 地址。
     *
     * @param ipAddress IPv4 或 IPv6 地址字符串
     */
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * 设置请求耗时。
     *
     * @param durationMs 耗时（毫秒）
     */
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 设置操作时间。
     *
     * @param createTime 操作时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
