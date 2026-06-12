package com.puchain.fep.security.impl.sign;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

/**
 * SM2 签名域参数（sm2p256v1，GB/T 32918）。包私有；与 security.impl.key.Sm2LoginCipher
 * 的 DOMAIN 同源（跨包不可见故各持 2 行，Simplify 候选注记于 Plan §共享工具表）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class Sm2SignSupport {

    /** SM2 推荐曲线参数。 */
    private static final X9ECParameters SM2_X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 椭圆曲线 domain 参数。 */
    static final ECDomainParameters DOMAIN = new ECDomainParameters(
            SM2_X9.getCurve(), SM2_X9.getG(), SM2_X9.getN(), SM2_X9.getH());

    private Sm2SignSupport() {
    }
}
