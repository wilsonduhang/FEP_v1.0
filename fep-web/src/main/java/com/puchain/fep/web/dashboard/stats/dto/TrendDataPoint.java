package com.puchain.fep.web.dashboard.stats.dto;

/**
 * A single data point in a trend chart.
 *
 * <p>Represents one time slot (hour or day) with sent and received counts.
 * See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TrendDataPoint {

    private String label;

    private long sentCount;

    private long receivedCount;

    /**
     * Default constructor.
     */
    public TrendDataPoint() {
        /* for serialization */
    }

    /**
     * Constructs a trend data point.
     *
     * @param label         time label (e.g. "00:00" or "2026-04-08")
     * @param sentCount     number of outbound messages
     * @param receivedCount number of inbound messages
     */
    public TrendDataPoint(final String label, final long sentCount,
                          final long receivedCount) {
        this.label = label;
        this.sentCount = sentCount;
        this.receivedCount = receivedCount;
    }

    /**
     * Returns the time label.
     *
     * @return label string
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the time label.
     *
     * @param label label string
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Returns the sent (outbound) message count.
     *
     * @return sent count
     */
    public long getSentCount() {
        return sentCount;
    }

    /**
     * Sets the sent (outbound) message count.
     *
     * @param sentCount sent count
     */
    public void setSentCount(final long sentCount) {
        this.sentCount = sentCount;
    }

    /**
     * Returns the received (inbound) message count.
     *
     * @return received count
     */
    public long getReceivedCount() {
        return receivedCount;
    }

    /**
     * Sets the received (inbound) message count.
     *
     * @param receivedCount received count
     */
    public void setReceivedCount(final long receivedCount) {
        this.receivedCount = receivedCount;
    }
}
