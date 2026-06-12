package com.puchain.fep.security.impl.sign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.security.impl.key.Sm2TestVectors;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * SM3withSM2 裸签 GB/T 32918.5-2017 附录 A 验签 KAT + sm-crypto 跨实现互操作。
 */
class SignServiceImplTest {

    private final SignServiceImpl signService = new SignServiceImpl();

    private static byte[] priv() {
        return HexFormat.of().parseHex(Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
    }

    private static byte[] pub() {
        return HexFormat.of().parseHex(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    }

    @Test
    void verify_gbt32918AppendixASignatureVector_returnsTrue() {
        final String sigBase64 = Base64.getEncoder().encodeToString(
                HexFormat.of().parseHex(Sm2TestVectors.GBT_SIGN_RS_HEX));
        assertThat(signService.verify(
                Sm2TestVectors.GBT_SIGN_PLAINTEXT.getBytes(StandardCharsets.US_ASCII),
                sigBase64, pub())).isTrue();
    }

    @Test
    void verify_smCryptoProducedSignature_returnsTrue() {
        final String sigBase64 = Base64.getEncoder().encodeToString(
                HexFormat.of().parseHex(Sm2TestVectors.SM_CRYPTO_SIGN_FIXTURE_RS_HEX));
        assertThat(signService.verify(
                Sm2TestVectors.SM_CRYPTO_SIGN_FIXTURE_PLAINTEXT.getBytes(StandardCharsets.UTF_8),
                sigBase64, pub())).isTrue();
    }

    @Test
    void signThenVerify_roundtrip_andRawRs64Bytes() {
        final byte[] data = "审计链 hash ✓ 2026".getBytes(StandardCharsets.UTF_8);
        final String sig = signService.sign(data, priv());
        assertThat(Base64.getDecoder().decode(sig)).hasSize(64);
        assertThat(signService.verify(data, sig, pub())).isTrue();
    }

    @Test
    void verify_tamperedDataOrSignature_returnsFalse() {
        final byte[] data = "row-hash".getBytes(StandardCharsets.UTF_8);
        final String sig = signService.sign(data, priv());
        final byte[] tamperedData = "row-hasH".getBytes(StandardCharsets.UTF_8);
        assertThat(signService.verify(tamperedData, sig, pub())).isFalse();
        final byte[] sigBytes = Base64.getDecoder().decode(sig);
        sigBytes[0] ^= 0x01;
        assertThat(signService.verify(data,
                Base64.getEncoder().encodeToString(sigBytes), pub())).isFalse();
    }

    @Test
    void malformedKeysOrNulls_throwIllegalArgument() {
        final byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> signService.sign(null, priv()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signService.sign(data, new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
        // 公钥长度违反（签名为格式合法 Base64 —— 单一约束）
        assertThatThrownBy(() -> signService.verify(data, "AAAA", new byte[64]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signService.verify(data, null, pub()))
                .isInstanceOf(IllegalArgumentException.class);
        // 单一约束：公钥合法 + 签名非 Base64 → false（值域错非参数错，不抛）
        assertThat(signService.verify(data, "not-base64_!", pub())).isFalse();
    }
}
