package com.puchain.fep.security.impl.key;

import java.math.BigInteger;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

/**
 * SM2 登录解密原语（BouncyCastle lightweight API，曲线 sm2p256v1，GB/T 32918）。
 *
 * <p>包私有：仅 KeyServiceImpl 经由本类触达 BC SM2 原语（ArchUnit R1：BC 仅
 * security.impl 包）。无 Spring stereotype。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class Sm2LoginCipher {

    /** SM2 推荐曲线参数（GB/T 32918.5）。 */
    private static final X9ECParameters SM2_X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 椭圆曲线 domain 参数。 */
    static final ECDomainParameters DOMAIN = new ECDomainParameters(
            SM2_X9.getCurve(), SM2_X9.getG(), SM2_X9.getN(), SM2_X9.getH());

    private Sm2LoginCipher() {
    }

    /**
     * 校验 [d]G 与公钥点配对一致（启动期配置完整性）。
     *
     * @param privateKeyHex 私钥标量 hex（64 字符）
     * @param publicKeyHex  公钥裸点 hex（130 字符，04 开头）
     * @return 配对一致返回 true
     */
    static boolean isMatchingKeyPair(final String privateKeyHex, final String publicKeyHex) {
        final BigInteger d = new BigInteger(privateKeyHex, 16);
        final ECPoint derived = DOMAIN.getG().multiply(d).normalize();
        // 字节级比较（Hex.decode 大小写兼容），规避字符串 case-mapping（IMPROPER_UNICODE）
        return java.util.Arrays.equals(derived.getEncoded(false), Hex.decode(publicKeyHex));
    }

    /**
     * 解密前端线格式 SM2 密文（sm-crypto C1C3C2，hex，无 04 前缀）。
     *
     * <p>GM S2a Task 3 实装；Task 2 阶段为编译衔接桩（同 Plan 内闭环替换）。</p>
     *
     * @param cipherHexNoPrefix C1C3C2 hex（无 04 前缀）
     * @param privateKeyHex     私钥标量 hex（64 字符）
     * @return 明文字节
     * @throws IllegalArgumentException 输入格式非法或解密失败
     */
    static byte[] decryptC1C3C2(final String cipherHexNoPrefix, final String privateKeyHex) {
        throw new UnsupportedOperationException("S2a T3");
    }
}
