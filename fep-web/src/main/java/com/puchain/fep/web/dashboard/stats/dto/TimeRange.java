package com.puchain.fep.web.dashboard.stats.dto;

/**
 * Time range enum for dashboard trend queries.
 *
 * <p>See PRD v1.3 section 5.2.5 Data Update Mechanism (FR-WEB-DASH-REFRESH).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TimeRange {

    /** Today (24 hourly data points). */
    TODAY,

    /** This week (7 daily data points). */
    THIS_WEEK,

    /** This month (days-in-month data points). */
    THIS_MONTH,

    /** Custom date range (daily data points between start and end). */
    CUSTOM
}
