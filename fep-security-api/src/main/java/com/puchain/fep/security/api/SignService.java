package com.puchain.fep.security.api;

/**
 * SM2 数字签名/验签服务接口。
 *
 * <p>使用 SM3withSM2 算法，签名结果为裸签 Base64 编码。</p>
 *
 * <p><strong>安全审核:</strong> 接口变更需安全工程师确认，实现类为 AI 禁入区域。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SignService {

    /**
     * SM2 签名。
     *
     * @param data       待签名数据，不可为 null
     * @param privateKey SM2 私钥（PKCS#8 编码），不可为 null
     * @return Base64 编码的签名值
     * @throws IllegalArgumentException 如果参数为 null
     */
    String sign(byte[] data, byte[] privateKey);

    /**
     * SM2 验签。
     *
     * @param data      原始数据，不可为 null
     * @param signature Base64 编码的签名值，不可为 null
     * @param publicKey SM2 公钥（X.509 编码），不可为 null
     * @return true 验签通过，false 验签失败
     * @throws IllegalArgumentException 如果参数为 null
     */
    boolean verify(byte[] data, String signature, byte[] publicKey);
}
