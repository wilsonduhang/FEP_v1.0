package com.puchain.fep.web.dashboard.stats.dto;

/**
 * Distribution item for message code breakdown.
 *
 * <p>Represents one slice in the business type distribution chart.
 * See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DistributionItem {

    private String messageCode;

    private String messageName;

    private long count;

    private double percentage;

    /**
     * Default constructor.
     */
    public DistributionItem() {
        /* for serialization */
    }

    /**
     * Constructs a distribution item.
     *
     * @param messageCode message code
     * @param messageName message name (display label)
     * @param count       record count
     * @param percentage  percentage of total (0.0 - 100.0)
     */
    public DistributionItem(final String messageCode,
                            final String messageName,
                            final long count,
                            final double percentage) {
        this.messageCode = messageCode;
        this.messageName = messageName;
        this.count = count;
        this.percentage = percentage;
    }

    /**
     * Returns the message code.
     *
     * @return message code
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Sets the message code.
     *
     * @param messageCode message code
     */
    public void setMessageCode(final String messageCode) {
        this.messageCode = messageCode;
    }

    /**
     * Returns the message name.
     *
     * @return message name
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * Sets the message name.
     *
     * @param messageName message name
     */
    public void setMessageName(final String messageName) {
        this.messageName = messageName;
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
