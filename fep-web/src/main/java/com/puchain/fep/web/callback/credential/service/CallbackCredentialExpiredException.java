package com.puchain.fep.web.callback.credential.service;

/**
 * 回调凭证已过期异常 — 当 TOKEN/OAUTH2 凭证 {@code expires_at} 早于当前时刻，
 * 解析期拒用，不静默降级为无鉴权。运维须更新凭证有效期或重配凭证。
 *
 * <p>镜像 {@link CallbackCredentialMissingException} 的非降级语义（FR-INFRA-CALLBACK-CREDENTIAL-EXPIRY）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CallbackCredentialExpiredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造过期异常。
     *
     * @param message 诊断信息（含 interfaceId，不含密文）
     */
    public CallbackCredentialExpiredException(final String message) {
        super(message);
    }
}
