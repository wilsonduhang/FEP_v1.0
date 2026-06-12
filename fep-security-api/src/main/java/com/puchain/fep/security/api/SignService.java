package com.puchain.fep.security.api;

/**
 * SM2 数字签名/验签服务接口（SM3withSM2，GB/T 32918.2）。
 *
 * <p>签名形态 = <strong>裸签 raw r∥s 64 字节的 Base64</strong>（PRD §3.3.1，非 DER）；
 * ZA 用户标识 = 默认 ID {@code 1234567812345678}（GB/T 32918 默认，与前端 sm-crypto
 * {@code hash:true} 默认一致）。</p>
 *
 * <p><strong>密钥字节形态（GM S5 实装定调）:</strong> impl provider 下 {@code privateKey} =
 * 32 字节标量 d 原始字节、{@code publicKey} = 65 字节未压缩裸点 04∥x∥y（BC lightweight
 * 直用，缩 ASN.1 面——与 S2a 登录密钥同理；原 PKCS#8/X.509 描述修正）。mock provider
 * 忽略密钥内容。</p>
 *
 * <p><strong>安全审核（🔓 2026-06-07 解禁治理）:</strong> 真实实现 {@code SignServiceImpl}
 * （fep-security-impl，GM S5）由 AI 编写 + 密码学专项 review + muzhou 签字；SM2 报文签验
 * wiring 与落地形态（外部签名验签服务器 1818 vs 进程内）待架构 §0.3 决策门定调（S2b）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SignService {

    /**
     * SM2 签名（SM3withSM2 裸签）。
     *
     * @param data       待签名数据，不可为 null
     * @param privateKey SM2 私钥——impl 为 32 字节标量 d 原始字节；mock 忽略内容，不可为 null
     * @return Base64 编码的裸签值（raw r∥s 64 字节）
     * @throws IllegalArgumentException 参数为 null 或私钥长度非法
     */
    String sign(byte[] data, byte[] privateKey);

    /**
     * SM2 验签。
     *
     * <p><strong>异常面（不对称，按用途设计）:</strong> 密钥长度/前缀等参数契约错抛
     * IllegalArgumentException（配置错误，响）；签名非法 Base64 / 公钥格式合法但非曲线点
     * 等值域错静默返回 false（验证失败）。调用方按此区分配置错误与验证失败。</p>
     *
     * @param data      原始数据，不可为 null
     * @param signature Base64 编码的裸签值，不可为 null
     * @param publicKey SM2 公钥——impl 为 65 字节未压缩裸点 04∥x∥y；mock 忽略内容，不可为 null
     * @return true 验签通过，false 验签失败
     * @throws IllegalArgumentException 参数为 null 或公钥长度/前缀非法
     */
    boolean verify(byte[] data, String signature, byte[] publicKey);
}
