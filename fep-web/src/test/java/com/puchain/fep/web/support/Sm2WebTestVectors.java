package com.puchain.fep.web.support;

/**
 * fep-web 测试侧 GB/T 32918.5-2017 附录 A 标准 SM2 测试密钥对单一源
 * （非真实密钥；跨模块 fep-security-impl 的 Sm2TestVectors test-classes 不可见故本模块复制）。
 *
 * <p>配对契约 [d]G=P：私钥与公钥成对，禁分散多源（改一忘改另一破坏验签）。
 * 消费方：{@code AuditIntegrityTestSupport}（审计验签）+
 * {@code Sm2LoginDecryptionProviderImplTest}（登录解密 @TestPropertySource，
 * public static final 编译期常量）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class Sm2WebTestVectors {

    /** GB/T 32918.5-2017 附录 A 标准测试私钥（32 字节 hex）。 */
    public static final String GBT_PRIV =
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";

    /** GB/T 32918.5-2017 附录 A 标准测试公钥（130-hex 未压缩裸点 04∥x∥y）。 */
    public static final String GBT_PUB =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    private Sm2WebTestVectors() {
    }
}
