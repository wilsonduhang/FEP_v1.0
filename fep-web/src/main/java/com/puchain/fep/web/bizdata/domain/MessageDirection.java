package com.puchain.fep.web.bizdata.domain;

/**
 * Message direction enum shared across bizdata modules.
 *
 * <p>Used by {@code BizMessageDefinition} (Task 5) and {@code BizMessageRecord} (Task 6).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageDirection {

    /** Outbound message (sent to HNDEMP). */
    OUTBOUND,

    /** Inbound message (received from HNDEMP). */
    INBOUND,

    /** Bidirectional message. */
    BIDIRECTIONAL
}
