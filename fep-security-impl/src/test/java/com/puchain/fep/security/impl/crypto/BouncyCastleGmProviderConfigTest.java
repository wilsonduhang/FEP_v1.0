package com.puchain.fep.security.impl.crypto;

import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 BouncyCastle GM provider 注册（SM2/SM3/SM4 JCE 算法可用前提）。
 */
class BouncyCastleGmProviderConfigTest {

    @Test
    void registersBouncyCastleProvider() {
        new BouncyCastleGmProviderConfig();
        assertThat(Security.getProvider("BC")).isNotNull();
    }

    @Test
    void registrationIsIdempotent() {
        new BouncyCastleGmProviderConfig();
        new BouncyCastleGmProviderConfig();
        assertThat(Security.getProvider("BC")).isNotNull();
    }
}
