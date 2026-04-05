package com.puchain.fep.common.exception;

import com.puchain.fep.common.domain.FepErrorCode;

/**
 * FEP 认证 / 授权异常。
 *
 * <p>由 GlobalExceptionHandler 统一转换为 HTTP 401/403。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class FepAuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FepErrorCode errorCode;

    /**
     * 使用错误码构造认证异常（消息取错误码默认消息）。
     *
     * @param errorCode 错误码
     */
    public FepAuthException(FepErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码和自定义消息构造认证异常。
     *
     * @param errorCode     错误码
     * @param customMessage 自定义消息
     */
    public FepAuthException(FepErrorCode errorCode, String customMessage) {
        super(customMessage);
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
