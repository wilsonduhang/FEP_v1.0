package com.puchain.fep.security.impl.gmvectors;

import com.puchain.fep.security.impl.crypto.BouncyCastleGmProviderConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB/T 32907-2016 SM4 标准测试向量验证 BouncyCastle SM4 合规（PRD §3.4.2 ECB/PKCS7）。
 *
 * <p>单块用 NoPadding 验国标原语（PKCS7 对完整块会追加整块填充改变结果）；
 * PKCS7 验业务 roundtrip。若单块 hex 不匹配 = BC SM4 有问题，禁改向量。</p>
 */
class Sm4GbtVectorTest {

    private static final byte[] KEY =
            HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");
    private static final byte[] PLAIN =
            HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");

    @BeforeAll
    static void registerBc() {
        new BouncyCastleGmProviderConfig();
    }

    @Test
    void sm4_ecb_singleBlock_matchesGbtStandardVector() throws Exception {
        final Cipher c = Cipher.getInstance("SM4/ECB/NoPadding", "BC");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "SM4"));
        final byte[] ct = c.doFinal(PLAIN);

        assertThat(HexFormat.of().formatHex(ct))
                .isEqualTo("681edf34d206965e86b3e94f536e4246");
    }

    @Test
    void sm4_ecbPkcs7_roundtrip_recoversPlaintext() throws Exception {
        final SecretKeySpec key = new SecretKeySpec(KEY, "SM4");
        final byte[] msg = "FEP 报文 xmlstr 敏感内容".getBytes(StandardCharsets.UTF_8);

        final Cipher enc = Cipher.getInstance("SM4/ECB/PKCS7Padding", "BC");
        enc.init(Cipher.ENCRYPT_MODE, key);
        final byte[] ct = enc.doFinal(msg);

        final Cipher dec = Cipher.getInstance("SM4/ECB/PKCS7Padding", "BC");
        dec.init(Cipher.DECRYPT_MODE, key);
        assertThat(dec.doFinal(ct)).isEqualTo(msg);
    }
}
