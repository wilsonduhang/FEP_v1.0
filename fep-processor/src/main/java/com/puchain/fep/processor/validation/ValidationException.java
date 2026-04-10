package com.puchain.fep.processor.validation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;

/**
 * XSD 校验阶段系统性异常（加载失败、输入超限、IO 错误等）。
 * 注意：字段级校验失败不抛此异常，而是返回 {@link ValidationResult#failed(java.util.List)}。
 */
public class ValidationException extends FepBusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用自定义消息构造校验异常。错误码固定 PROC_8501。
     *
     * @param customMessage 自定义消息
     */
    public ValidationException(final String customMessage) {
        super(FepErrorCode.PROC_8501, customMessage);
    }

    /**
     * 使用自定义消息和原因构造校验异常。错误码固定 PROC_8501。
     *
     * @param customMessage 自定义消息
     * @param cause         原因
     */
    public ValidationException(final String customMessage, final Throwable cause) {
        super(FepErrorCode.PROC_8501, customMessage, cause);
    }
}
