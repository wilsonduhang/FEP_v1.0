package com.puchain.fep.transport;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the FEP Transport module.
 *
 * <p>Bound to the {@code fep.transport.*} namespace. Provides sensible defaults
 * that match the HNDEMP integration requirements.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.transport")
public class TransportProperties {

    /** Default institution code placeholder. */
    public static final String DEFAULT_INSTITUTION_CODE = "DEFAULT_INST_00";

    /** Default TLQ real-time channel port. */
    public static final int DEFAULT_REALTIME_PORT = 20001;

    /** Default TLQ batch channel port. */
    public static final int DEFAULT_BATCH_PORT = 20002;

    /** Default maximum retry attempts. */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /** Default retry base delay in milliseconds. */
    public static final long DEFAULT_RETRY_BASE_DELAY_MS = 1000L;

    /** Default deduplication cache capacity. */
    public static final int DEFAULT_DEDUP_CAPACITY = 10000;

    /**
     * 14-character institution code used in TLQ queue naming and routing.
     */
    private String institutionCode = DEFAULT_INSTITUTION_CODE;

    /**
     * TLQ real-time channel port number.
     */
    private int realtimePort = DEFAULT_REALTIME_PORT;

    /**
     * TLQ batch channel port number.
     */
    private int batchPort = DEFAULT_BATCH_PORT;

    /**
     * Maximum number of retry attempts for failed message sends.
     */
    private int maxRetries = DEFAULT_MAX_RETRIES;

    /**
     * Base delay in milliseconds for exponential retry backoff.
     */
    private long retryBaseDelayMs = DEFAULT_RETRY_BASE_DELAY_MS;

    /**
     * Maximum capacity of the in-memory message deduplication cache.
     */
    private int dedupCapacity = DEFAULT_DEDUP_CAPACITY;

    /**
     * Return the institution code.
     *
     * @return the institution code, never {@code null}
     */
    public String getInstitutionCode() {
        return institutionCode;
    }

    /**
     * Set the institution code.
     *
     * @param institutionCode the institution code to set
     */
    public void setInstitutionCode(final String institutionCode) {
        this.institutionCode = institutionCode;
    }

    /**
     * Return the real-time channel port.
     *
     * @return the real-time port number
     */
    public int getRealtimePort() {
        return realtimePort;
    }

    /**
     * Set the real-time channel port.
     *
     * @param realtimePort the port number to set
     */
    public void setRealtimePort(final int realtimePort) {
        this.realtimePort = realtimePort;
    }

    /**
     * Return the batch channel port.
     *
     * @return the batch port number
     */
    public int getBatchPort() {
        return batchPort;
    }

    /**
     * Set the batch channel port.
     *
     * @param batchPort the port number to set
     */
    public void setBatchPort(final int batchPort) {
        this.batchPort = batchPort;
    }

    /**
     * Return the maximum number of retries.
     *
     * @return the max retries count
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Set the maximum number of retries.
     *
     * @param maxRetries the max retries to set
     */
    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Return the base delay for retry backoff in milliseconds.
     *
     * @return the retry base delay in milliseconds
     */
    public long getRetryBaseDelayMs() {
        return retryBaseDelayMs;
    }

    /**
     * Set the base delay for retry backoff in milliseconds.
     *
     * @param retryBaseDelayMs the delay to set
     */
    public void setRetryBaseDelayMs(final long retryBaseDelayMs) {
        this.retryBaseDelayMs = retryBaseDelayMs;
    }

    /**
     * Return the deduplication cache capacity.
     *
     * @return the dedup capacity
     */
    public int getDedupCapacity() {
        return dedupCapacity;
    }

    /**
     * Set the deduplication cache capacity.
     *
     * @param dedupCapacity the capacity to set
     */
    public void setDedupCapacity(final int dedupCapacity) {
        this.dedupCapacity = dedupCapacity;
    }
}
