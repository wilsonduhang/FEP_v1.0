package com.puchain.fep.web.dashboard.stats.dto;

import java.math.BigDecimal;

/**
 * Dashboard statistics cards response DTO.
 *
 * <p>Contains the four key metrics displayed on the home page dashboard:
 * total transaction amount, success count, today's message count, and
 * exception count. See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class StatsCardsResponse {

    private BigDecimal totalAmount;

    private long successCount;

    private long todayMessageCount;

    private long exceptionCount;

    /**
     * Returns the cumulative transaction amount.
     *
     * @return total amount (never null, {@link BigDecimal#ZERO} when no data)
     */
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    /**
     * Sets the cumulative transaction amount.
     *
     * @param totalAmount total amount
     */
    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Returns the number of successfully processed messages.
     *
     * @return success count
     */
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * Sets the number of successfully processed messages.
     *
     * @param successCount success count
     */
    public void setSuccessCount(final long successCount) {
        this.successCount = successCount;
    }

    /**
     * Returns the number of messages created today.
     *
     * @return today message count
     */
    public long getTodayMessageCount() {
        return todayMessageCount;
    }

    /**
     * Sets the number of messages created today.
     *
     * @param todayMessageCount today message count
     */
    public void setTodayMessageCount(final long todayMessageCount) {
        this.todayMessageCount = todayMessageCount;
    }

    /**
     * Returns the number of failed (exception) messages.
     *
     * @return exception count
     */
    public long getExceptionCount() {
        return exceptionCount;
    }

    /**
     * Sets the number of failed (exception) messages.
     *
     * @param exceptionCount exception count
     */
    public void setExceptionCount(final long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }
}
