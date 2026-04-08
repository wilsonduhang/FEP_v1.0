package com.puchain.fep.web.submission.dashboard.dto;

/**
 * Dashboard statistics response DTO.
 *
 * <p>Aggregates counts from output interfaces, data sources,
 * and submission records for the submission management overview page.
 * See PRD v1.3 section 5.5.1 Data Overview (FR-WEB-SUB-DASH).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DashboardResponse {

    private long totalInterfaceCount;

    private long enabledInterfaceCount;

    private long totalDataSourceCount;

    private long totalRecordCount;

    private long pushedRecordCount;

    private long pendingRecordCount;

    /**
     * Returns the total number of output interfaces.
     *
     * @return total interface count
     */
    public long getTotalInterfaceCount() {
        return totalInterfaceCount;
    }

    /**
     * Sets the total number of output interfaces.
     *
     * @param totalInterfaceCount total interface count
     */
    public void setTotalInterfaceCount(final long totalInterfaceCount) {
        this.totalInterfaceCount = totalInterfaceCount;
    }

    /**
     * Returns the number of enabled output interfaces.
     *
     * @return enabled interface count
     */
    public long getEnabledInterfaceCount() {
        return enabledInterfaceCount;
    }

    /**
     * Sets the number of enabled output interfaces.
     *
     * @param enabledInterfaceCount enabled interface count
     */
    public void setEnabledInterfaceCount(final long enabledInterfaceCount) {
        this.enabledInterfaceCount = enabledInterfaceCount;
    }

    /**
     * Returns the total number of data sources.
     *
     * @return total data source count
     */
    public long getTotalDataSourceCount() {
        return totalDataSourceCount;
    }

    /**
     * Sets the total number of data sources.
     *
     * @param totalDataSourceCount total data source count
     */
    public void setTotalDataSourceCount(final long totalDataSourceCount) {
        this.totalDataSourceCount = totalDataSourceCount;
    }

    /**
     * Returns the total number of submission records.
     *
     * @return total record count
     */
    public long getTotalRecordCount() {
        return totalRecordCount;
    }

    /**
     * Sets the total number of submission records.
     *
     * @param totalRecordCount total record count
     */
    public void setTotalRecordCount(final long totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    /**
     * Returns the number of pushed submission records.
     *
     * @return pushed record count
     */
    public long getPushedRecordCount() {
        return pushedRecordCount;
    }

    /**
     * Sets the number of pushed submission records.
     *
     * @param pushedRecordCount pushed record count
     */
    public void setPushedRecordCount(final long pushedRecordCount) {
        this.pushedRecordCount = pushedRecordCount;
    }

    /**
     * Returns the number of pending submission records.
     *
     * @return pending record count
     */
    public long getPendingRecordCount() {
        return pendingRecordCount;
    }

    /**
     * Sets the number of pending submission records.
     *
     * @param pendingRecordCount pending record count
     */
    public void setPendingRecordCount(final long pendingRecordCount) {
        this.pendingRecordCount = pendingRecordCount;
    }
}
