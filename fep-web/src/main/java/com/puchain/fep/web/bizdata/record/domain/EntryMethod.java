package com.puchain.fep.web.bizdata.record.domain;

/**
 * Entry method enum indicating how a message record was created.
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum EntryMethod {

    /** Created via API integration. */
    API,

    /** Manually entered through the web interface. */
    MANUAL
}
