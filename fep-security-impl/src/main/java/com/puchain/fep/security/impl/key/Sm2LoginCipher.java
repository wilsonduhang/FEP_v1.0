package com.puchain.fep.security.impl.key;

import java.math.BigInteger;
import java.util.Arrays;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.DecoderException;
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

    /** 前端线格式最小 hex 长度：C1(128) + C3(64) + C2(≥2)。 */
    private static final int MIN_CIPHER_HEX_LENGTH = 194;

    /** 未压缩点标识字节。 */
    private static final byte UNCOMPRESSED_POINT_PREFIX = 0x04;

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
        return Arrays.equals(derived.getEncoded(false), Hex.decode(publicKeyHex));
    }

    /**
     * 解密前端线格式 SM2 密文（sm-crypto C1C3C2，hex，无 04 前缀——
     * fep-admin-ui sm2-cipher.ts 契约，内部统一补 0x04 后喂 BC SM2Engine）。
     *
     * @param cipherHexNoPrefix C1C3C2 hex（≥194 字符，无 04 前缀）
     * @param privateKeyHex     私钥标量 hex（64 字符）
     * @return 明文字节
     * @throws IllegalArgumentException 输入格式非法或解密失败（消息不含密文/明文）
     */
    static byte[] decryptC1C3C2(final String cipherHexNoPrefix, final String privateKeyHex) {
        if (cipherHexNoPrefix == null || cipherHexNoPrefix.length() < MIN_CIPHER_HEX_LENGTH
                || cipherHexNoPrefix.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "SM2 ciphertext must be even-length hex of at least "
                            + MIN_CIPHER_HEX_LENGTH + " chars (C1C3C2, no 04 prefix)");
        }
        final byte[] c1c3c2;
        try {
            c1c3c2 = Hex.decode(cipherHexNoPrefix);
        } catch (final DecoderException e) {
            throw new IllegalArgumentException("SM2 ciphertext is not valid hex", e);
        }
        final byte[] withPrefix = new byte[c1c3c2.length + 1];
        withPrefix[0] = UNCOMPRESSED_POINT_PREFIX;
        System.arraycopy(c1c3c2, 0, withPrefix, 1, c1c3c2.length);
        final SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        engine.init(false, new ECPrivateKeyParameters(
                new BigInteger(privateKeyHex, 16), DOMAIN));
        try {
            return engine.processBlock(withPrefix, 0, withPrefix.length);
        } catch (final InvalidCipherTextException e) {
            throw new IllegalArgumentException("SM2 login password decryption failed", e);
        }
    }
}
