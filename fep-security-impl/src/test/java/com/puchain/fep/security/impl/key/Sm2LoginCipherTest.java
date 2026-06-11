package com.puchain.fep.security.impl.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

/**
 * SM2 解密 GB/T 32918.5-2017 附录 A 标准向量（推荐曲线加密示例）+ sm-crypto 跨实现互操作验证。
 */
class Sm2LoginCipherTest {

    @Test
    void decrypt_gbt32918AppendixAVector_recoversEncryptionStandard() {
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                Sm2TestVectors.GBT_CIPHER_C1C3C2_NO_PREFIX_HEX,
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.US_ASCII))
                .isEqualTo(Sm2TestVectors.GBT_PLAINTEXT);
    }

    @Test
    void decrypt_smCryptoProducedCipher_recoversLoginPassword() {
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX,
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.UTF_8))
                .isEqualTo(Sm2TestVectors.SM_CRYPTO_FIXTURE_PLAINTEXT);
    }

    @Test
    void decrypt_bcSelfEncryptedRoundtrip_recoversUtf8Plaintext() throws Exception {
        final String message = "FEP 登录密码 Roundtrip ✓ 2026";
        final SM2Engine encryptEngine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        final var pubPoint = Sm2LoginCipher.DOMAIN.getCurve().decodePoint(
                Hex.decode(Sm2TestVectors.GBT_PUBLIC_KEY_HEX));
        encryptEngine.init(true, new ParametersWithRandom(
                new ECPublicKeyParameters(pubPoint, Sm2LoginCipher.DOMAIN), new SecureRandom()));
        final byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        final byte[] cipherWithPrefix = encryptEngine.processBlock(msg, 0, msg.length);
        // BC 输出带 04 前缀 → 去前缀模拟前端线格式
        final String wireHex = Hex.toHexString(cipherWithPrefix).substring(2);
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                wireHex, Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.UTF_8)).isEqualTo(message);
    }

    @Test
    void decrypt_tamperedC3_throwsWithoutLeakingData() {
        final String cipher = Sm2TestVectors.GBT_CIPHER_C1C3C2_NO_PREFIX_HEX;
        // C3 区间 = [128, 192)，翻转其第 1 个字符
        final char original = cipher.charAt(128);
        final char flipped = original == '5' ? '6' : '5';
        final String tampered = cipher.substring(0, 128) + flipped + cipher.substring(129);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2(
                tampered, Sm2TestVectors.GBT_PRIVATE_KEY_HEX))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage())
                            .doesNotContain(tampered)
                            .doesNotContain("encryption standard");
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        assertThat(e.getCause().getMessage())
                                .doesNotContain(tampered)
                                .doesNotContain("encryption standard");
                    }
                });
    }

    @Test
    void decrypt_malformedInput_throwsIllegalArgument() {
        final String priv = Sm2TestVectors.GBT_PRIVATE_KEY_HEX;
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2(null, priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("zz" + "00".repeat(96), priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("0".repeat(193), priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("00".repeat(64), priv))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
