package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OAuth2TokenResponse} 单元测试 — 验证 record 字段访问 + Jackson 蛇形键反序列化。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OAuth2TokenResponseTest {

    @Test
    void recordHoldsTokenAndExpiry() {
        final OAuth2TokenResponse r = new OAuth2TokenResponse("abc", 3600, "Bearer");
        assertThat(r.accessToken()).isEqualTo("abc");
        assertThat(r.expiresIn()).isEqualTo(3600);
        assertThat(r.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void deserializesSnakeCaseJson() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final OAuth2TokenResponse r = mapper.readValue(
                "{\"access_token\":\"tok-xyz\",\"expires_in\":7200,\"token_type\":\"Bearer\"}",
                OAuth2TokenResponse.class);
        assertThat(r.accessToken()).isEqualTo("tok-xyz");
        assertThat(r.expiresIn()).isEqualTo(7200);
        assertThat(r.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void toStringMasksAccessToken() {
        final OAuth2TokenResponse r = new OAuth2TokenResponse("super-secret-token", 3600, "Bearer");
        assertThat(r.toString()).doesNotContain("super-secret-token");
    }
}
