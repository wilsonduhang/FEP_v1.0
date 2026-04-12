package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.auth.domain.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link LoginVerifier}.
 *
 * <p>Covers three password resolution paths: plaintext, SM2-encrypted,
 * and missing (both fields blank).</p>
 */
@ExtendWith(MockitoExtension.class)
class LoginVerifierTest {

    @Mock
    private KeyService keyService;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private LoginVerifier verifier;

    @Test
    void resolveClearPassword_shouldReturnPlaintextWhenNoEncryptedField() {
        LoginRequest req = new LoginRequest();
        req.setPassword("Abc12345");

        assertThat(verifier.resolveClearPassword(req)).isEqualTo("Abc12345");
        verifyNoInteractions(keyService);
    }

    @Test
    void resolveClearPassword_shouldDecryptWhenEncryptedFieldPresent() {
        String encrypted = Base64.getEncoder().encodeToString("Abc12345".getBytes(UTF_8));
        LoginRequest req = new LoginRequest();
        req.setEncryptedPassword(encrypted);
        req.setKeyId("mock-key-v1");
        given(keyService.decryptLoginPassword(encrypted, "mock-key-v1"))
                .willReturn("Abc12345");

        assertThat(verifier.resolveClearPassword(req)).isEqualTo("Abc12345");
    }

    @Test
    void resolveClearPassword_shouldThrowWhenBothPasswordsBlank() {
        LoginRequest req = new LoginRequest();
        req.setPassword("");

        assertThatThrownBy(() -> verifier.resolveClearPassword(req))
                .isInstanceOf(FepBusinessException.class);
    }
}
