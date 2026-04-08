package com.puchain.fep.web.bizdata.record.dto;

/**
 * Summary item for message record aggregation by message code.
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RecordSummaryItem {

    /** Message code. */
    private final String messageCode;

    /** Message name (from BizMessageDefinition). */
    private final String messageName;

    /** Total record count for this message code. */
    private final long totalCount;

    /** Count of SUCCESS records. */
    private final long successCount;

    /** Count of PENDING records. */
    private final long pendingCount;

    /** Count of FAILED records. */
    private final long failedCount;

    /**
     * Construct a RecordSummaryItem.
     *
     * @param messageCode  message code
     * @param messageName  message name
     * @param totalCount   total record count
     * @param successCount success count
     * @param pendingCount pending count
     * @param failedCount  failed count
     */
    public RecordSummaryItem(final String messageCode,
                             final String messageName,
                             final long totalCount,
                             final long successCount,
                             final long pendingCount,
                             final long failedCount) {
        this.messageCode = messageCode;
        this.messageName = messageName;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.pendingCount = pendingCount;
        this.failedCount = failedCount;
    }

    /**
     * Get message code.
     *
     * @return message code
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Get message name.
     *
     * @return message name
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * Get total count.
     *
     * @return total count
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Get success count.
     *
     * @return success count
     */
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Get pending count.
     *
     * @return pending count
     */
    public long getPendingCount() {
        return pendingCount;
    }

    /**
     * Get failed count.
     *
     * @return failed count
     */
    public long getFailedCount() {
        return failedCount;
    }
}
