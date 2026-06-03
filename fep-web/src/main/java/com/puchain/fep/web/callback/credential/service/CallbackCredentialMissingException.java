package com.puchain.fep.web.callback.credential.service;

/**
 * 回调凭证缺失异常 — TOKEN / OAUTH2 鉴权类型的接口在 {@code callback_credential} 表无对应记录时抛出。
 *
 * <p>由 {@link CallbackCredentialResolver} 在解析鉴权头时抛出，表示接口配置为需鉴权
 * 但凭证未落库（运维配置遗漏 / 凭证被误删）。调用方（{@code CallbackHttpClient}）应据此
 * 终止本次回调并记录失败，不应静默降级为无鉴权请求。</p>
 *
 * <p>参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CallbackCredentialMissingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常。
     *
     * @param message 异常说明（含 interfaceId 以便定位）
     */
    public CallbackCredentialMissingException(final String message) {
        super(message);
    }
}
