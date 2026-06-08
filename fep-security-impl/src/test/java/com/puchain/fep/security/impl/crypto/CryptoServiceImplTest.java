package com.puchain.fep.security.impl.crypto;

import com.puchain.fep.security.api.CryptoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CryptoServiceImpl SM4/ECB/PKCS7 真实加解密验证（GB/T 32907-2016 合规 + 边界）。
 *
 * <p>向量值为国标真值，不匹配=实现有问题，禁改向量迁就实现。</p>
 */
class CryptoServiceImplTest {

    private static final byte[] GBT_KEY =
            HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");

    private final CryptoService crypto = new CryptoServiceImpl();

    @BeforeAll
    static void registerBc() {
        new BouncyCastleGmProviderConfig();
    }

    @Test
    void encryptThenDecrypt_recoversGbtStandardPlaintext() {
        final byte[] plain = HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");
        final byte[] cipher = crypto.encrypt(plain, GBT_KEY);
        // PKCS7 对完整 16 字节块追加整块填充 → 密文 32 字节
        assertThat(cipher).hasSize(32);
        assertThat(crypto.decrypt(cipher, GBT_KEY)).isEqualTo(plain);
    }

    @Test
    void sm4SingleBlock_matchesGbtStandardVector_selfContainedAnchor() throws Exception {
        // C3：T1 自包含国标锚定 — 用 service 同款 "BC" provider 的 SM4/ECB/NoPadding 验单块
        // 已知答案（GB/T 32907-2016 标准向量），与 S0 Sm4GbtVectorTest 同值，确认 BC SM4 原语合规。
        final javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("SM4/ECB/NoPadding", "BC");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(GBT_KEY, "SM4"));
        final byte[] singleBlock = c.doFinal(
                HexFormat.of().parseHex("0123456789abcdeffedcba9876543210"));
        assertThat(HexFormat.of().formatHex(singleBlock))
                .isEqualTo("681edf34d206965e86b3e94f536e4246");
    }

    @Test
    void roundtrip_utf8MessageContent_recoversPlaintext() {
        final byte[] plain = "FEP 报文 xmlstr 敏感内容 ¥123.45".getBytes(StandardCharsets.UTF_8);
        assertThat(crypto.decrypt(crypto.encrypt(plain, GBT_KEY), GBT_KEY)).isEqualTo(plain);
    }

    @Test
    void encrypt_nullPlaintext_throws() {
        assertThatThrownBy(() -> crypto.encrypt(null, GBT_KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encrypt_nullKey_throws() {
        assertThatThrownBy(() -> crypto.encrypt("x".getBytes(StandardCharsets.UTF_8), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encrypt_wrongKeyLength_throws() {
        assertThatThrownBy(() -> crypto.encrypt(
                "x".getBytes(StandardCharsets.UTF_8), new byte[15]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrypt_wrongKeyLength_throws() {
        assertThatThrownBy(() -> crypto.decrypt(new byte[32], new byte[17]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
