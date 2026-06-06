package com.puchain.fep.web.callback.credential.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackCredentialEntity} 有效期（expires_at）行为测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackCredentialEntityTest {

    private static final LocalDateTime EXPIRY = LocalDateTime.of(2030, 1, 1, 0, 0);

    @Test
    void newToken_carriesExpiresAt() {
        final CallbackCredentialEntity e = CallbackCredentialEntity.newToken(
                "if1", new byte[]{1, 2, 3}, "Authorization", "k1", EXPIRY);
        assertThat(e.getExpiresAt()).isEqualTo(EXPIRY);
    }

    @Test
    void newToken_nullExpiresAt_meansNeverExpire() {
        final CallbackCredentialEntity e = CallbackCredentialEntity.newToken(
                "if1", new byte[]{1, 2, 3}, "Authorization", "k1", null);
        assertThat(e.getExpiresAt()).isNull();
    }

    @Test
    void newOauth_carriesExpiresAt() {
        final CallbackCredentialEntity e = CallbackCredentialEntity.newOauth(
                "if1", new byte[]{1}, new byte[]{2}, "https://token", "scope", "k1", EXPIRY);
        assertThat(e.getExpiresAt()).isEqualTo(EXPIRY);
    }

    @Test
    void updateExpiresAt_nonNull_updates() {
        final CallbackCredentialEntity e = CallbackCredentialEntity.newToken(
                "if1", new byte[]{1, 2, 3}, "Authorization", "k1", null);
        e.updateExpiresAt(EXPIRY);
        assertThat(e.getExpiresAt()).isEqualTo(EXPIRY);
    }

    @Test
    void updateExpiresAt_null_keepsOriginal() {
        final CallbackCredentialEntity e = CallbackCredentialEntity.newToken(
                "if1", new byte[]{1, 2, 3}, "Authorization", "k1", EXPIRY);
        e.updateExpiresAt(null);
        assertThat(e.getExpiresAt()).isEqualTo(EXPIRY);
    }
}
