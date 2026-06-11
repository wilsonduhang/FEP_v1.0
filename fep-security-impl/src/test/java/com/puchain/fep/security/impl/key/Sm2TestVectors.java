package com.puchain.fep.security.impl.key;

/**
 * GB/T 32918.5-2017 附录 A 推荐曲线（sm2p256v1）标准测试向量（公开标准数据，非生产密钥）+
 * sm-crypto@0.3.13 跨实现互操作 fixture（2026-06-10 生成，见 Plan §设计背景）。
 */
final class Sm2TestVectors {

    /** GB/T 32918.5-2017 附录 A 私钥 dB（32 字节标量 hex）。 */
    static final String GBT_PRIVATE_KEY_HEX =
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";

    /** GB/T 32918.5-2017 附录 A 公钥（04∥x∥y 130 hex）。 */
    static final String GBT_PUBLIC_KEY_HEX =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    /** GB/T 32918.5-2017 附录 A 密文（C1C3C2，前端线格式无 04 前缀），明文 "encryption standard"。 */
    static final String GBT_CIPHER_C1C3C2_NO_PREFIX_HEX =
            "04ebfc718e8d1798620432268e77feb6415e2ede0e073c0f4f640ecd2e149a73"
                    + "e858f9d81e5430a57b36daab8f950a3c64e6ee6a63094d99283aff767e124df0"
                    + "59983c18f809e262923c53aec295d30383b54e39d609d160afcb1908d0bd8766"
                    + "21886ca989ca9c7d58087307ca93092d651efa";

    /** GB/T 32918.5-2017 附录 A 明文。 */
    static final String GBT_PLAINTEXT = "encryption standard";

    /** sm-crypto@0.3.13 doEncrypt(msg, GBT_PUBLIC, cipherMode=1) 实测 fixture，明文 Sm2@LoginPwd2026。 */
    static final String SM_CRYPTO_FIXTURE_CIPHER_HEX =
            "7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349"
                    + "613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9"
                    + "c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371c"
                    + "d5cf6ea9209ecf782d390ce6fcc1733f";

    /** sm-crypto fixture 明文。 */
    static final String SM_CRYPTO_FIXTURE_PLAINTEXT = "Sm2@LoginPwd2026";

    private Sm2TestVectors() {
    }
}
