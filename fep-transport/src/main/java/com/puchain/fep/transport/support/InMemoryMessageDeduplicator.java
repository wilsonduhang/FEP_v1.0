package com.puchain.fep.transport.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory LRU implementation of {@link MessageDeduplicator}.
 *
 * <p>Uses a {@link LinkedHashMap} wrapped with {@link Collections#synchronizedMap} to provide
 * thread-safe, insertion-ordered deduplication with automatic eviction of the oldest entry
 * once the configured capacity is exceeded.</p>
 *
 * <p>This implementation is suitable for single-node deployments. For clustered environments,
 * consider a Redis-backed implementation.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class InMemoryMessageDeduplicator implements MessageDeduplicator {

    /** Sentinel value stored in the map (only keys matter). */
    private static final Boolean PRESENT = Boolean.TRUE;

    /** Default load factor for the underlying {@link LinkedHashMap}. */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Map<String, Boolean> seen;

    /**
     * Create a new deduplicator with the specified maximum capacity.
     *
     * <p>When the number of recorded message identifiers exceeds {@code maxCapacity},
     * the oldest (least-recently-inserted) entry is automatically evicted.</p>
     *
     * @param maxCapacity the maximum number of message identifiers to retain; must be positive
     */
    public InMemoryMessageDeduplicator(final int maxCapacity) {
        this.seen = Collections.synchronizedMap(
                new LinkedHashMap<String, Boolean>(maxCapacity, DEFAULT_LOAD_FACTOR, false) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean removeEldestEntry(final Map.Entry<String, Boolean> eldest) {
                        return size() > maxCapacity;
                    }
                }
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is thread-safe.</p>
     */
    @Override
    public boolean isDuplicate(final String msgId) {
        return seen.putIfAbsent(msgId, PRESENT) != null;
    }
}
