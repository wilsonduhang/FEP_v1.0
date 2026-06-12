package com.puchain.fep.security.impl.key;

/**
 * GB/T 32918.5-2017 附录 A 推荐曲线（sm2p256v1）标准测试向量（公开标准数据，非生产密钥）+
 * sm-crypto@0.3.13 跨实现互操作 fixture（2026-06-10 生成，见 Plan §设计背景）。
 *
 * <p>S5 起 public（供 security.impl.sign 包 SignServiceImplTest 复用）。
 * fep-web {@code Sm2LoginDecryptionProviderImplTest} 持有同 fixture 字面值副本
 * （跨模块不引 test-jar），fixture 再生成时须同步两处。</p>
 */
public final class Sm2TestVectors {

    /** GB/T 32918.5-2017 附录 A 私钥 dB（32 字节标量 hex）。 */
    public static final String GBT_PRIVATE_KEY_HEX =
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";

    /** GB/T 32918.5-2017 附录 A 公钥（04∥x∥y 130 hex）。 */
    public static final String GBT_PUBLIC_KEY_HEX =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    /** GB/T 32918.5-2017 附录 A 密文（C1C3C2，前端线格式无 04 前缀），明文 "encryption standard"。 */
    public static final String GBT_CIPHER_C1C3C2_NO_PREFIX_HEX =
            "04ebfc718e8d1798620432268e77feb6415e2ede0e073c0f4f640ecd2e149a73"
                    + "e858f9d81e5430a57b36daab8f950a3c64e6ee6a63094d99283aff767e124df0"
                    + "59983c18f809e262923c53aec295d30383b54e39d609d160afcb1908d0bd8766"
                    + "21886ca989ca9c7d58087307ca93092d651efa";

    /** GB/T 32918.5-2017 附录 A 明文。 */
    public static final String GBT_PLAINTEXT = "encryption standard";

    /** sm-crypto@0.3.13 doEncrypt(msg, GBT_PUBLIC, cipherMode=1) 实测 fixture，明文 Sm2@LoginPwd2026。 */
    public static final String SM_CRYPTO_FIXTURE_CIPHER_HEX =
            "7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349"
                    + "613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9"
                    + "c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371c"
                    + "d5cf6ea9209ecf782d390ce6fcc1733f";

    /** sm-crypto fixture 明文。 */
    public static final String SM_CRYPTO_FIXTURE_PLAINTEXT = "Sm2@LoginPwd2026";

    /** GB/T 32918.5-2017 附录 A 签名示例明文。 */
    public static final String GBT_SIGN_PLAINTEXT = "message digest";

    /** GB/T 32918.5-2017 附录 A 签名 r∥s（raw 128 hex；2026-06-12 sm-crypto 独立验签仲裁 GREEN）。 */
    public static final String GBT_SIGN_RS_HEX =
            "f5a03b0648d2c4630eeac513e1bb81a15944da3827d5b74143ac7eaceee720b3"
                    + "b1b6aa29df212fd8763182bc0d421ca1bb9038fd1f7f42d4840b69c485bbc1aa";

    /** sm-crypto@0.3.13 doSignature({hash:true,der:false}) 实测签名 fixture 明文。 */
    public static final String SM_CRYPTO_SIGN_FIXTURE_PLAINTEXT = "audit-hash-0123456789abcdef";

    /** sm-crypto 签名 fixture r∥s（128 hex，2026-06-12 生成冻结，self-verify ✓）。 */
    public static final String SM_CRYPTO_SIGN_FIXTURE_RS_HEX =
            "4eb778ecc265d4ccf7f27e0e1db0e63ab03c4c50496613b7517527acb36a049e"
                    + "29963e2a443dee34a589e0968fa1ca0b918751c40a8a9e5c414541a31d5d1ee1";

    private Sm2TestVectors() {
    }
}
