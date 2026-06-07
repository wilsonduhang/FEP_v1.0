package com.puchain.fep.security.impl.gmvectors;

import com.puchain.fep.security.impl.crypto.BouncyCastleGmProviderConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB/T 32905-2016 SM3 标准测试向量验证 BouncyCastle SM3 合规。
 *
 * <p>若 hex 不匹配 = BC SM3 有问题，立即排查，禁改向量迁就实现。</p>
 */
class Sm3GbtVectorTest {

    @BeforeAll
    static void registerBc() {
        new BouncyCastleGmProviderConfig();
    }

    @Test
    void sm3_abc_matchesGbtStandardVector() throws Exception {
        final MessageDigest md = MessageDigest.getInstance("SM3", "BC");
        final byte[] digest = md.digest("abc".getBytes(StandardCharsets.US_ASCII));

        assertThat(digest).hasSize(32);
        assertThat(HexFormat.of().formatHex(digest))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }

    @Test
    void sm3_64byteMultiBlock_matchesGbtStandardVector() throws Exception {
        // GB/T 32905-2016 第二标准向量（"abcd"×16 = 64 字节，测多块迭代 + 长度编码边界）
        final byte[] input = "abcd".repeat(16).getBytes(StandardCharsets.US_ASCII);
        final MessageDigest md = MessageDigest.getInstance("SM3", "BC");
        assertThat(HexFormat.of().formatHex(md.digest(input)))
                .isEqualTo("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732");
    }
}
