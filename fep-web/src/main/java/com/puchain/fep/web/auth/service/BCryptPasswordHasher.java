package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.security.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码散列实现。
 *
 * <p>strength=12 (工作因子)，单次散列约 200-300 ms。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final int STRENGTH = 12;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("rawPassword must not be null or empty");
        }
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedValue) {
        if (rawPassword == null || hashedValue == null) {
            return false;
        }
        return encoder.matches(rawPassword, hashedValue);
    }
}
