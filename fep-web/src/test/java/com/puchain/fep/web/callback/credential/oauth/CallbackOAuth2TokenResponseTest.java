package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackOAuth2TokenResponse} 单元测试 — 验证 record 字段访问 + Jackson 蛇形键反序列化。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackOAuth2TokenResponseTest {

    @Test
    void recordHoldsTokenAndExpiry() {
        final CallbackOAuth2TokenResponse r = new CallbackOAuth2TokenResponse("abc", 3600, "Bearer");
        assertThat(r.accessToken()).isEqualTo("abc");
        assertThat(r.expiresIn()).isEqualTo(3600);
        assertThat(r.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void deserializesSnakeCaseJson() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final CallbackOAuth2TokenResponse r = mapper.readValue(
                "{\"access_token\":\"tok-xyz\",\"expires_in\":7200,\"token_type\":\"Bearer\"}",
                CallbackOAuth2TokenResponse.class);
        assertThat(r.accessToken()).isEqualTo("tok-xyz");
        assertThat(r.expiresIn()).isEqualTo(7200);
        assertThat(r.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void toStringMasksAccessToken() {
        final CallbackOAuth2TokenResponse r = new CallbackOAuth2TokenResponse("super-secret-token", 3600, "Bearer");
        assertThat(r.toString()).doesNotContain("super-secret-token");
    }
}
