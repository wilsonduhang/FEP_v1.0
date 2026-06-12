package com.puchain.fep.security.api;

/**
 * SM4 对称加解密服务接口。
 *
 * <p>使用 SM4/ECB/PKCS7Padding 模式，密钥由密钥管理模块提供。</p>
 *
 * <p><strong>安全审核（🔓 2026-06-07 解禁治理）:</strong> 接口变更经密码学专项 review +
 * muzhou 签字；真实实现 {@code CryptoServiceImpl}（fep-security-impl）已由 AI 编写 +
 * 密码学专项 review（SM4 S1 实装）；真实密钥材料永不入 repo，部署期注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CryptoService {

    /**
     * SM4 加密。
     *
     * @param plaintext 明文字节数组，不可为 null
     * @param key       SM4 密钥（16 字节），不可为 null
     * @return 密文字节数组
     * @throws IllegalArgumentException 如果参数为 null 或密钥长度不是 16 字节
     */
    byte[] encrypt(byte[] plaintext, byte[] key);

    /**
     * SM4 解密。
     *
     * @param ciphertext 密文字节数组，不可为 null
     * @param key        SM4 密钥（16 字节），不可为 null
     * @return 明文字节数组
     * @throws IllegalArgumentException 如果参数为 null 或密钥长度不是 16 字节
     */
    byte[] decrypt(byte[] ciphertext, byte[] key);
}
