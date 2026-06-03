package com.puchain.fep.web.callback.credential.oauth;

/**
 * OAuth2 token 获取的可重试失败（5xx / IO / 超时 / 中断）。
 *
 * <p>调用方应纳入回调 retry/DLQ 重试路径。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CallbackOAuth2RetryableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 失败描述
     */
    public CallbackOAuth2RetryableException(final String message) {
        super(message);
    }

    /**
     * @param message 失败描述
     * @param cause   底层异常
     */
    public CallbackOAuth2RetryableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
