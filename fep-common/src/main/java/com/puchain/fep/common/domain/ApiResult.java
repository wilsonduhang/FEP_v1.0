package com.puchain.fep.common.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FEP 统一 API 响应体。
 *
 * <p>参见 PRD v1.3 §9.1 全局异常处理。</p>
 *
 * <p>JSON 结构示例:</p>
 * <pre>{@code
 * {
 *   "code": "200",
 *   "message": "成功",
 *   "data": { ... },
 *   "traceId": "20260406143000-000001",
 *   "timestamp": "2026-04-06T14:30:00+08:00"
 * }
 * }</pre>
 *
 * @param <T> 业务数据类型
 * @author FEP Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public final class ApiResult<T> {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final String code;
    private final String message;
    private final T data;
    private final String traceId;
    private final String timestamp;

    private ApiResult(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = currentTraceId();
        this.timestamp = OffsetDateTime.now().format(TS_FORMATTER);
    }

    /**
     * 成功（无数据）。
     *
     * @return 200 响应
     */
    public static ApiResult<Void> success() {
        return new ApiResult<>(FepErrorCode.SUCCESS.getCode(), FepErrorCode.SUCCESS.getDefaultMessage(), null);
    }

    /**
     * 成功（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 200 响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(FepErrorCode.SUCCESS.getCode(), FepErrorCode.SUCCESS.getDefaultMessage(), data);
    }

    /**
     * 失败（使用错误码默认消息）。
     *
     * @param errorCode 错误码
     * @return 错误响应
     */
    public static ApiResult<Void> failure(FepErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    /**
     * 失败（自定义消息）。
     *
     * @param errorCode      错误码
     * @param customMessage  自定义消息
     * @return 错误响应
     */
    public static ApiResult<Void> failure(FepErrorCode errorCode, String customMessage) {
        return new ApiResult<>(errorCode.getCode(), customMessage, null);
    }

    private static String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "";
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
