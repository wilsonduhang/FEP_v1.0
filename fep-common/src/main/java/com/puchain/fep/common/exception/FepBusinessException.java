package com.puchain.fep.common.exception;

import com.puchain.fep.common.domain.FepErrorCode;

/**
 * FEP 业务异常基类。
 *
 * <p>所有业务逻辑校验失败应抛出本异常或其子类，由 GlobalExceptionHandler 统一转换为 HTTP 400/409。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class FepBusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FepErrorCode errorCode;

    /**
     * 使用错误码构造业务异常（消息取错误码默认消息）。
     *
     * @param errorCode 错误码
     */
    public FepBusinessException(FepErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码和自定义消息构造业务异常。
     *
     * @param errorCode     错误码
     * @param customMessage 自定义消息
     */
    public FepBusinessException(FepErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码、自定义消息和原因构造业务异常。
     *
     * @param errorCode     错误码
     * @param customMessage 自定义消息
     * @param cause         原因
     */
    public FepBusinessException(FepErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码枚举
     */
    public FepErrorCode getErrorCode() {
        return errorCode;
    }
}
