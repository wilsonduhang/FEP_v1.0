package com.puchain.fep.web.dashboard.stats.dto;

/**
 * Distribution item for message processing status breakdown.
 *
 * <p>Represents one slice in the status distribution chart.
 * See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class StatusDistributionItem {

    private String status;

    private long count;

    private double percentage;

    /**
     * Default constructor.
     */
    public StatusDistributionItem() {
        /* for serialization */
    }

    /**
     * Constructs a status distribution item.
     *
     * @param status     status name
     * @param count      record count
     * @param percentage percentage of total (0.0 - 100.0)
     */
    public StatusDistributionItem(final String status, final long count,
                                  final double percentage) {
        this.status = status;
        this.count = count;
        this.percentage = percentage;
    }

    /**
     * Returns the status name.
     *
     * @return status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status name.
     *
     * @param status status
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * Returns the record count.
     *
     * @return count
     */
    public long getCount() {
        return count;
    }

    /**
     * Sets the record count.
     *
     * @param count count
     */
    public void setCount(final long count) {
        this.count = count;
    }

    /**
     * Returns the percentage of total.
     *
     * @return percentage (0.0 - 100.0)
     */
    public double getPercentage() {
        return percentage;
    }

    /**
     * Sets the percentage of total.
     *
     * @param percentage percentage
     */
    public void setPercentage(final double percentage) {
        this.percentage = percentage;
    }
}
