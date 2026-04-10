package com.puchain.fep.converter.exception;

import com.puchain.fep.common.domain.FepErrorCode;

import java.util.Objects;

/**
 * 报文转换层业务异常。封装 {@link FepErrorCode} 和上下文信息。
 *
 * <p>所有报文转换层抛出的检查点异常（XML 序列化、签名范围提取、压缩加解密、
 * TLQ payload 超限等）统一使用本类，并通过 {@code errorCode} 将机读错误码与
 * 全局错误码体系对齐。参见 PRD v1.3 §9.3。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessageConverterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final FepErrorCode errorCode;

    /**
     * 构造异常。
     *
     * @param code   错误码
     * @param detail 上下文描述（禁止包含敏感数据）
     */
    public MessageConverterException(final FepErrorCode code, final String detail) {
        this(code, detail, null);
    }

    /**
     * 构造异常并携带根因。
     *
     * @param code   错误码
     * @param detail 上下文描述（禁止包含敏感数据）
     * @param cause  根因异常
     */
    public MessageConverterException(final FepErrorCode code, final String detail, final Throwable cause) {
        super(formatMessage(code, detail), cause);
        this.errorCode = code;
    }

    private static String formatMessage(final FepErrorCode code, final String detail) {
        Objects.requireNonNull(code, "errorCode must not be null");
        return code.getCode() + ": " + detail;
    }

    /**
     * 返回关联的错误码。
     *
     * @return 错误码枚举
     */
    public FepErrorCode getErrorCode() {
        return errorCode;
    }
}
