package com.puchain.fep.transport.support;

import com.puchain.fep.common.util.FepConstants;

import java.util.Objects;

/**
 * Resolves TLQ queue names based on PRD section 3.1.2 naming conventions.
 *
 * <p>Each institution communicates with HNDEMP via 9 standard queues.
 * Queue names embed the institution code (local queues, dead letter) or the
 * HNDEMP centre node code (remote, destination, send queues).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class QueueNameResolver {

    /** HNDEMP centre node code. R-2 (2026-05-07): 转引用 {@link FepConstants#HNDEMP_NODE_CODE}。 */
    public static final String HNDEMP_CODE = FepConstants.HNDEMP_NODE_CODE;

    private final String institutionCode;

    /**
     * Queue types defined by PRD section 3.1.2.
     */
    public enum QueueType {
        /** Local real-time queue. */
        REALTIME_LOCAL,
        /** Local batch queue. */
        BATCH_LOCAL,
        /** Remote real-time queue. */
        REALTIME_REMOTE,
        /** Remote batch queue. */
        BATCH_REMOTE,
        /** Destination real-time queue. */
        REALTIME_DEST,
        /** Destination batch queue. */
        BATCH_DEST,
        /** Send real-time queue. */
        REALTIME_SEND,
        /** Send batch queue. */
        BATCH_SEND,
        /** Dead letter queue. */
        DEAD_LETTER
    }

    /**
     * Create a resolver for the given institution.
     *
     * @param institutionCode the institution code used in queue names
     * @throws NullPointerException if institutionCode is null
     */
    public QueueNameResolver(final String institutionCode) {
        this.institutionCode = Objects.requireNonNull(institutionCode, "institutionCode must not be null");
    }

    /**
     * Resolve the full queue name for the given queue type.
     *
     * @param queueType the queue type to resolve
     * @return the fully qualified queue name
     * @throws NullPointerException if queueType is null
     */
    public String resolve(final QueueType queueType) {
        Objects.requireNonNull(queueType, "queueType must not be null");
        return switch (queueType) {
            case REALTIME_LOCAL  -> "QLOCAL." + institutionCode + ".REAL.1";
            case BATCH_LOCAL     -> "QLOCAL." + institutionCode + ".BATCH.1";
            case REALTIME_REMOTE -> "QREMOTE." + HNDEMP_CODE + ".REAL.1";
            case BATCH_REMOTE    -> "QREMOTE." + HNDEMP_CODE + ".BATCH.1";
            case REALTIME_DEST   -> "QLOCAL." + HNDEMP_CODE + ".REAL.1";
            case BATCH_DEST      -> "QLOCAL." + HNDEMP_CODE + ".BATCH.1";
            case REALTIME_SEND   -> "QSEND." + HNDEMP_CODE + ".REAL.1";
            case BATCH_SEND      -> "QSEND." + HNDEMP_CODE + ".BATCH.1";
            case DEAD_LETTER     -> "QDEAD." + institutionCode;
        };
    }

    /**
     * Resolve the QCU (Queue Connection Unit) name for this institution.
     *
     * @return the QCU name in format {@code QCU_HNDEMP_{institutionCode}_1}
     */
    public String resolveQcu() {
        return "QCU_HNDEMP_" + institutionCode + "_1";
    }
}
