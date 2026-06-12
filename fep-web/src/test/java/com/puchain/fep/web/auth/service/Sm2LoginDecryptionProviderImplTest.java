package com.puchain.fep.web.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.web.auth.domain.LoginRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * GM S2a provider=impl 全 context 登录解密链路 IT（镜像 S1
 * CallbackLegacyCredentialMigrationTest 范式）。
 *
 * <p>密钥字面值 = GB/T 32918.5-2017 附录 A 公开标准测试向量（非生产密钥）；
 * 密文 fixture 由前端 sm-crypto@0.3.13 doEncrypt(cipherMode=1) 实测生成，
 * 验证生产真实路径（sm-crypto 产文 → BC 解密）。命名 {@code *Test} 确保 Surefire 收录。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210",
        "fep.security.sm2.login-active-key-id=sm2-login-v1",
        "fep.security.sm2.login-keys.sm2-login-v1.private-key-hex="
                + "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8",
        "fep.security.sm2.login-keys.sm2-login-v1.public-key-hex="
                + "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13"
})
class Sm2LoginDecryptionProviderImplTest {

    /** sm-crypto@0.3.13 实测 fixture（同 fep-security-impl Sm2TestVectors，跨模块重复注明来源）。 */
    private static final String SM_CRYPTO_CIPHER_HEX =
            "7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349"
                    + "613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9"
                    + "c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371c"
                    + "d5cf6ea9209ecf782d390ce6fcc1733f";

    @Autowired
    private KeyService keyService;

    @Autowired
    private HashService hashService;

    @Autowired
    private LoginVerifier loginVerifier;

    // provider=impl 下 SignService（SM2 报文签名）属 S2b 未实现、mock 已门控关 → 桩补使全 context 启动
    @MockBean
    private SignService signService;

    @Test
    void publicKeyDistribution_returnsRawPointBase64AndLoginKeyId() {
        assertThat(keyService.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
        final byte[] point = Base64.getDecoder().decode(keyService.getSm2PublicKeyBase64());
        assertThat(point).hasSize(65);
        assertThat(point[0]).isEqualTo((byte) 0x04);
    }

    @Test
    void decryptLoginPassword_smCryptoWireFormat_recoversPlaintext() {
        assertThat(keyService.decryptLoginPassword(SM_CRYPTO_CIPHER_HEX, "sm2-login-v1"))
                .isEqualTo("Sm2@LoginPwd2026");
    }

    @Test
    void loginVerifier_resolveClearPassword_decryptsViaRealKeyService() {
        final LoginRequest request = new LoginRequest();
        request.setEncryptedPassword(SM_CRYPTO_CIPHER_HEX);
        request.setKeyId("sm2-login-v1");
        assertThat(loginVerifier.resolveClearPassword(request)).isEqualTo("Sm2@LoginPwd2026");
    }

    @Test
    void sm4CredentialKeyId_staysIndependentFromSm2LoginKeyId() {
        assertThat(keyService.getKeyId()).isEqualTo("sm4-cred-v1");
        assertThat(keyService.getSm2LoginKeyId()).isNotEqualTo(keyService.getKeyId());
    }

    @Test
    void hashService_alwaysOn_availableUnderImplProvider() {
        assertThat(hashService.sm3Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }
}
