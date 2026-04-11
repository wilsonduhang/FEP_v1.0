package com.puchain.fep.processor.state;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;

/**
 * 报文处理状态机非法转移异常。
 *
 * <p>当 {@link MessageStateMachine} 检测到非法的状态转移（例如从终态
 * {@link MessageProcessStatus#COMPLETED} 试图继续变更，或越阶从
 * {@link MessageProcessStatus#RECEIVED} 直接进入 {@link MessageProcessStatus#PROCESSING}），
 * 抛出本异常。错误码统一为 {@link FepErrorCode#PROC_8502}，由
 * {@code GlobalExceptionHandler} 转换为 HTTP 409。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class IllegalMessageStateException extends FepBusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 以预格式化的描述消息构造异常。消息中不得包含敏感数据，
     * 调用方应先通过 {@code LogSanitizer.sanitize} 清洗。
     *
     * @param message 已脱敏的异常描述
     */
    public IllegalMessageStateException(final String message) {
        super(FepErrorCode.PROC_8502, message);
    }
}
