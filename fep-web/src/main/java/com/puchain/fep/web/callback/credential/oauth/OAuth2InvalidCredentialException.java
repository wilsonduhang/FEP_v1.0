package com.puchain.fep.web.callback.credential.oauth;

/**
 * OAuth2 token endpoint 拒绝凭证（401 / 403）— 不可重试。
 *
 * <p>表示 client_id/client_secret 配置错误或被吊销，重试无意义；调用方应告警并停止重试。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class OAuth2InvalidCredentialException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 失败描述
     */
    public OAuth2InvalidCredentialException(final String message) {
        super(message);
    }
}
