package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.auth.domain.LoginRequest;
import org.springframework.stereotype.Component;

/**
 * Handles login credential verification: captcha, password resolution
 * (plaintext or SM2-encrypted), password hash matching, and failure tracking.
 *
 * <p>Extracted from {@code AuthService} to reduce its constructor dependency
 * count below the ArchUnit 7-parameter limit. The orchestration logic
 * (account locking, status changes, token issuance) remains in AuthService.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 * @see AuthService
 */
@Component
public class LoginVerifier {

    private final KeyService keyService;
    private final PasswordHasher passwordHasher;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;

    /**
     * Constructs a LoginVerifier.
     *
     * @param keyService          SM2 key management for decrypting encrypted passwords
     * @param passwordHasher      password hashing service for verification
     * @param captchaService      captcha generation and verification
     * @param loginAttemptService login failure tracking
     */
    public LoginVerifier(final KeyService keyService,
                         final PasswordHasher passwordHasher,
                         final CaptchaService captchaService,
                         final LoginAttemptService loginAttemptService) {
        this.keyService = keyService;
        this.passwordHasher = passwordHasher;
        this.captchaService = captchaService;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Extracts the cleartext password from the login request.
     *
     * <p>If {@code encryptedPassword} is present and non-blank, decrypts it
     * via {@link KeyService#decryptLoginPassword}; otherwise falls back to
     * the plaintext {@code password} field. Throws if both are absent.</p>
     *
     * @param request the login request
     * @return cleartext password
     * @throws FepBusinessException if neither password field is provided
     */
    public String resolveClearPassword(final LoginRequest request) {
        if (request.getEncryptedPassword() != null && !request.getEncryptedPassword().isBlank()) {
            return keyService.decryptLoginPassword(
                    request.getEncryptedPassword(), request.getKeyId());
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new FepBusinessException(FepErrorCode.AUTH_0402, "密码不能为空");
        }
        return request.getPassword();
    }

    /**
     * Verifies that the cleartext password matches the stored hash.
     *
     * @param clearPassword cleartext password to verify
     * @param storedHash    stored password hash
     * @return {@code true} if the password matches
     */
    public boolean verifyPassword(final String clearPassword, final String storedHash) {
        return passwordHasher.matches(clearPassword, storedHash);
    }

    /**
     * Verifies and consumes a captcha code.
     *
     * @param captchaId captcha identifier
     * @param userInput user-provided captcha code
     * @return {@code true} if captcha is valid
     */
    public boolean verifyCaptcha(final String captchaId, final String userInput) {
        return captchaService.verifyAndConsume(captchaId, userInput);
    }

    /**
     * Records a login failure for the given account.
     *
     * @param account the login account
     * @return cumulative failure count
     */
    public int recordFailure(final String account) {
        return loginAttemptService.recordFailure(account);
    }

    /**
     * Clears login failure count (called on successful login).
     *
     * @param account the login account
     */
    public void clearFailures(final String account) {
        loginAttemptService.clearFailures(account);
    }

    /**
     * Returns the maximum allowed login failures before lockout.
     *
     * @return max failure attempts
     */
    public int getMaxAttempts() {
        return loginAttemptService.getMaxAttempts();
    }
}
