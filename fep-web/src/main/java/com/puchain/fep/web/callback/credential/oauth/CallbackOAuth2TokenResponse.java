package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth2 token endpoint 响应（RFC 6749 §5.1 Access Token Response）。
 *
 * <p>仅映射 FEP 回调凭证所需的 3 字段；其余 IDP 扩展字段忽略。
 * {@code access_token} 是敏感运行时凭证，{@link #toString()} 显式屏蔽其值，
 * 防止经日志 / 异常消息泄漏（Step 5.5 安全审计 C.3）。</p>
 *
 * @param accessToken 行内系统签发的 access_token（敏感）
 * @param expiresIn   有效期秒数
 * @param tokenType   token 类型（通常 {@code Bearer}）
 * @author FEP Team
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CallbackOAuth2TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("token_type") String tokenType) {

    /**
     * 屏蔽 {@code accessToken} 明文的 {@code toString()}，仅暴露非敏感元数据。
     *
     * @return 不含 access_token 明文的字符串表示
     */
    @Override
    public String toString() {
        return "CallbackOAuth2TokenResponse{accessToken=***, expiresIn=" + expiresIn
                + ", tokenType=" + tokenType + '}';
    }
}
