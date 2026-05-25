package com.puchain.fep.web.messageinbound.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AckIdempotencyKeys} — namespaced 9120-ack idempotency key
 * derivation shared by all inbound 9120-ack listeners (2101/3112/3105/3009/3103/3113).
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("AckIdempotencyKeys — 命名空间 9120-ack 幂等 key")
class AckIdempotencyKeysTest {

    @Test
    @DisplayName("derive == SHA-256(ACK-9120-<msgNo>-<serialNo>)[0:32]")
    void derive_matchesNamespacedSha256() throws Exception {
        assertThat(AckIdempotencyKeys.derive("3112", "SN1")).isEqualTo(expected("3112", "SN1"));
        assertThat(AckIdempotencyKeys.derive("2101", "T1")).isEqualTo(expected("2101", "T1"));
    }

    @Test
    @DisplayName("不同 msgNo 同 serialNo → 不同 key（命名空间隔离）")
    void derive_differentMsgNoSameSerial_differentKey() {
        assertThat(AckIdempotencyKeys.derive("3105", "S")).isNotEqualTo(AckIdempotencyKeys.derive("3009", "S"));
    }

    @Test
    @DisplayName("derive 输出长度恒为 32 hex")
    void derive_is32Hex() {
        assertThat(AckIdempotencyKeys.derive("3103", "SN20260525")).hasSize(32).matches("[0-9a-f]{32}");
    }

    private static String expected(final String msgNo, final String serialNo) throws Exception {
        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        final byte[] hash = sha256.digest(("ACK-9120-" + msgNo + "-" + serialNo).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash).substring(0, 32);
    }
}
