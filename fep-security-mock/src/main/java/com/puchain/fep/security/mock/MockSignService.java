package com.puchain.fep.security.mock;

import com.puchain.fep.security.api.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * SM2 签名验签的 Mock 实现 — 固定签名值。
 *
 * <p>仅用于开发和测试环境。签名始终返回 "MOCK_SIGNATURE"，验签始终返回 true。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Profile("dev")
public class MockSignService implements SignService {

    private static final Logger log = LoggerFactory.getLogger(MockSignService.class);

    /** Mock 签名固定返回值。 */
    public static final String MOCK_SIGNATURE = "MOCK_SIGNATURE";

    @Override
    public String sign(byte[] data, byte[] privateKey) {
        if (data == null || privateKey == null) {
            throw new IllegalArgumentException("data and privateKey must not be null");
        }
        log.debug("[MOCK] SM2 sign called, data length={}", data.length);
        return MOCK_SIGNATURE;
    }

    @Override
    public boolean verify(byte[] data, String signature, byte[] publicKey) {
        if (data == null || signature == null || publicKey == null) {
            throw new IllegalArgumentException("data, signature and publicKey must not be null");
        }
        // 清理日志输入中的 CR/LF 防止日志注入 (CWE-117)
        final String safeSignature = signature.replaceAll("[\r\n]", "_");
        log.debug("[MOCK] SM2 verify called, data length={}, signature={}", data.length, safeSignature);
        return true;
    }
}
