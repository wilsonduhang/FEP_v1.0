package com.puchain.fep.web.messageinbound.listener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 9120 ack 幂等 key 派生 util（共享于所有 inbound 9120-ack listener）。
 *
 * <p>key = {@code SHA-256("ACK-9120-" + msgNo + "-" + serialNo)[0:32]}。按源报文 msgNo
 * 命名空间隔离，确保不同报文类型（2101/3112/3105/3009/3103/3113）在共享业务 serialNo
 * 空间下不碰撞 {@code outbound_message_queue.uk_outbound_queue_idempotency_key} UNIQUE
 * 约束（避免一条 9120 ack 被静默吞掉丢失）。同源报文重传则收敛为同一 key（去重）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class AckIdempotencyKeys {

    private static final String PREFIX = "ACK-9120-";
    private static final int HEX_LEN = 32;

    private AckIdempotencyKeys() {
    }

    /**
     * Derives the deterministic 32-hex idempotency key for a 9120 ack.
     *
     * @param msgNo    the source inbound message number (e.g. {@code "3112"}), non-null
     * @param serialNo the inbound business serial number, non-null
     * @return 32-char lowercase hex string
     */
    public static String derive(final String msgNo, final String serialNo) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha256.digest(
                    (PREFIX + msgNo + "-" + serialNo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, HEX_LEN);
        } catch (final NoSuchAlgorithmException nsa) {
            throw new IllegalStateException("SHA-256 algorithm missing on JVM", nsa);
        }
    }
}
