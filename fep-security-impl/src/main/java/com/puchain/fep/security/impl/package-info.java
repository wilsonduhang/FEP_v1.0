/**
 * FEP 安全模块国密真实实现（BouncyCastle SM2/SM3/SM4）。
 *
 * <p><strong>2026-06-07 治理变更</strong>：muzhou 授权 AI 进入国密安全域开发。
 * 本包国密实现严格遵 PRD v1.3 §3.3（SM2 签名）/§3.4（SM4 加密）/§8.3（安全）+ 国标
 * GB/T 32905（SM3）/32907（SM4）/32918（SM2），用标准测试向量逐字节验证。</p>
 *
 * <p><strong>密钥材料红线（不变）</strong>：真实 SM2 私钥 / SM4 主密钥永不入 repo/git，
 * 部署期经 envelope-encrypted 配置 / sealed key store / HSM 注入；dev/CI 用 mock 或
 * GB/T 国标测试密钥。</p>
 *
 * <p>S0 阶段仅含 {@code crypto.BouncyCastleGmProviderConfig}（GM provider 注册）+ GB/T
 * 测试向量；CryptoServiceImpl / SignServiceImpl / KeyServiceImpl 真实实现留 S1/S2。</p>
 *
 * @since 1.0.0
 */
package com.puchain.fep.security.impl;
