package com.puchain.fep.transport.support;

/**
 * Message deduplication contract for TLQ transport layer.
 *
 * <p>Implementations track previously seen message identifiers and report whether a given
 * message has already been processed. The first call with a particular {@code msgId} records
 * the identifier and returns {@code false} (not a duplicate); subsequent calls with the same
 * {@code msgId} return {@code true}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface MessageDeduplicator {

    /**
     * Check if the given message identifier has already been seen.
     *
     * <p>The first invocation for a particular {@code msgId} registers it and returns
     * {@code false}. All subsequent invocations with the same {@code msgId} return
     * {@code true}.</p>
     *
     * @param msgId the unique message identifier; must not be {@code null}
     * @return {@code true} if the message was already recorded (duplicate),
     *         {@code false} if this is the first occurrence
     */
    boolean isDuplicate(String msgId);
}
