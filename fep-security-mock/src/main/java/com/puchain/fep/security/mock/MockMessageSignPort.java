package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.MessageSignPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link MessageSignPort} 的 Mock 实现 — 固定签名值、验签恒 true（仅 dev/CI）。
 *
 * <p><strong>@Component（非 @Service）:</strong> NamingConventionTest 约束 @Service→{@code *Service}
 * / @Configuration→{@code *Configuration}，本类名以 {@code *Port} 结尾，故用无命名约束的
 * @Component（v0.2 MAJOR-5）。{@code @ConditionalOnProperty(provider=mock, matchIfMissing=true)}
 * 与 impl 侧 {@code BcMessageSignPort}（{@code GmSecurityConfiguration @Bean}，provider=impl 门控）
 * 互斥单 bean。</p>
 *
 * <p><strong>⚠️ 假绿显式化（密码学 MINOR-1）:</strong> {@link #verify} 恒返回 true，verify-inbound
 * 开关开启时<strong>不提供完整性保证</strong>——仅 dev/CI mock 链路。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class MockMessageSignPort implements MessageSignPort {

    private static final Logger log = LoggerFactory.getLogger(MockMessageSignPort.class);

    /** Mock 报文签名固定返回值。 */
    public static final String MOCK_MSG_SIGN = "MOCK_MSG_SIGN";

    /** 构造期告警：mock 验签恒 true，不提供完整性保证（密码学 MINOR-1）。 */
    public MockMessageSignPort() {
        log.warn("[MOCK] MessageSignPort active: verify() always returns true; verify-inbound "
                + "provides NO integrity guarantee under mock provider (dev/CI only)");
    }

    @Override
    public String sign(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        log.debug("[MOCK] message sign called, data length={}", data.length);
        return MOCK_MSG_SIGN;
    }

    @Override
    public boolean verify(final byte[] data, final String signatureBase64, final String srcNode) {
        if (data == null || signatureBase64 == null || srcNode == null) {
            throw new IllegalArgumentException(
                    "data, signatureBase64 and srcNode must not be null");
        }
        return true;
    }
}
