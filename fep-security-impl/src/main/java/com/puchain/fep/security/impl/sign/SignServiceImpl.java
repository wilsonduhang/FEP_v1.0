package com.puchain.fep.security.impl.sign;

import com.puchain.fep.security.api.SignService;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.PlainDSAEncoding;
import org.bouncycastle.crypto.signers.SM2Signer;

/**
 * SM3withSM2 裸签实现（GB/T 32918.2；BC SM2Signer + PlainDSAEncoding = raw r∥s 64 字节）。
 *
 * <p>ZA 用户标识 = BC 默认 ID {@code 1234567812345678}（GB/T 32918 默认，与前端
 * sm-crypto {@code hash:true} 默认一致）。私钥 = 32 字节标量 d 原始字节；公钥 = 65 字节
 * 未压缩裸点 04∥x∥y。无 Spring stereotype，经 GmSecurityConfiguration @Bean 注册
 * （provider=impl 门控）。S5 服务审计行签名；S2b 报文签验 wiring 与落地形态另由 §0.3 定调。</p>
 *
 * <p><strong>异常面（不对称，按用途设计）：</strong>密钥长度/前缀等参数契约错抛
 * IllegalArgumentException（响——配置错误）；签名非法 Base64 / 公钥格式合法但非曲线点等
 * 值域错静默返回 false（验证失败）。调用方按此区分配置错误与验证失败。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class SignServiceImpl implements SignService {

    /** 私钥标量长度（字节）。 */
    private static final int PRIVATE_KEY_LENGTH = 32;

    /** 未压缩裸点公钥长度（字节）。 */
    private static final int PUBLIC_KEY_LENGTH = 65;

    /** 未压缩点标识字节。 */
    private static final byte UNCOMPRESSED_POINT_PREFIX = 0x04;

    @Override
    public String sign(final byte[] data, final byte[] privateKey) {
        requireData(data);
        if (privateKey == null || privateKey.length != PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM2 private key must be 32-byte raw scalar d");
        }
        final SM2Signer signer = new SM2Signer(PlainDSAEncoding.INSTANCE);
        signer.init(true, new ParametersWithRandom(new ECPrivateKeyParameters(
                new BigInteger(1, privateKey), Sm2SignSupport.DOMAIN), new SecureRandom()));
        signer.update(data, 0, data.length);
        try {
            return Base64.getEncoder().encodeToString(signer.generateSignature());
        } catch (final CryptoException e) {
            throw new IllegalStateException("SM2 signature generation failed", e);
        }
    }

    @Override
    public boolean verify(final byte[] data, final String signature, final byte[] publicKey) {
        requireData(data);
        if (signature == null) {
            throw new IllegalArgumentException("signature must not be null");
        }
        if (publicKey == null || publicKey.length != PUBLIC_KEY_LENGTH
                || publicKey[0] != UNCOMPRESSED_POINT_PREFIX) {
            throw new IllegalArgumentException(
                    "SM2 public key must be 65-byte uncompressed point (04||x||y)");
        }
        final byte[] rawSignature;
        try {
            rawSignature = Base64.getDecoder().decode(signature);
        } catch (final IllegalArgumentException e) {
            return false;
        }
        final SM2Signer signer = new SM2Signer(PlainDSAEncoding.INSTANCE);
        try {
            signer.init(false, new ECPublicKeyParameters(
                    Sm2SignSupport.DOMAIN.getCurve().decodePoint(publicKey),
                    Sm2SignSupport.DOMAIN));
        } catch (final IllegalArgumentException e) {
            return false;
        }
        signer.update(data, 0, data.length);
        return signer.verifySignature(rawSignature);
    }

    private static void requireData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
    }
}
