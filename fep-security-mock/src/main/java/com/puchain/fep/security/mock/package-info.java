/**
 * FEP 安全模块 Mock 实现。
 *
 * <p>仅用于开发和测试环境，通过 {@code @Profile("dev")} 激活。
 * 加密 = 明文透传，签名 = 固定值 "MOCK_SIGNATURE"。</p>
 *
 * <p>生产环境必须替换为 fep-security-impl 真实国密实现。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
package com.puchain.fep.security.mock;
