package com.puchain.fep.web.bizdata.record.domain;

/**
 * Message processing status enum.
 *
 * <p>Tracks the lifecycle of a business message record.
 * See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageProcessStatus {

    /** Awaiting processing. */
    PENDING,

    /** Currently being processed. */
    PROCESSING,

    /** Successfully processed. */
    SUCCESS,

    /** Processing failed. */
    FAILED
}
