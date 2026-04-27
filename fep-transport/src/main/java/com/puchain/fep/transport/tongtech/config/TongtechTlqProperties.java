package com.puchain.fep.transport.tongtech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Tongtech TongLINK/Q (TLQ) real SDK adapter.
 *
 * <p>Bound to the {@code fep.transport.tongtech.*} namespace. Default values align with
 * {@code docs/plans/2026-04-26-p1c-sdk-validation-and-decisions.md §5.5}, which captures
 * the empirical defaults expected by the TLQ 8.1.15.2_p6 broker for FEP local development.</p>
 *
 * <p>Field semantics map to TLQ SDK arguments documented in §5.5 of the SDK validation memo:
 * <ul>
 *   <li>{@code brokerHost / brokerPort / brokerId / qcuName} identify the target broker.</li>
 *   <li>{@code userName / password} authenticate the client (broker credentials,
 *       not user accounts; never persisted to the FEP DB schema).</li>
 *   <li>{@code connTimeSec / replyTmoutSec} bound connection and reply waits.</li>
 *   <li>{@code secExitFlag} toggles the broker-side security exit hook.</li>
 *   <li>{@code consumerPollIntervalMs} drives the {@code TongtechTlqConsumer} polling loop.</li>
 *   <li>{@code adminHost / adminPort} target the optional admin endpoint for node lifecycle.</li>
 * </ul></p>
 *
 * <p>This class only provides configuration values; bean activation is handled by
 * {@link TongtechTransportConfiguration} (only loaded when
 * {@code fep.transport.provider=tongtech}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.transport.tongtech")
public class TongtechTlqProperties {

    /** Default broker host (loopback for local dev). */
    public static final String DEFAULT_BROKER_HOST = "127.0.0.1";

    /** Default broker port (TLQ 8.1.15.2_p6 standard listener). */
    public static final int DEFAULT_BROKER_PORT = 10024;

    /** Default broker numeric identifier. */
    public static final int DEFAULT_BROKER_ID = 1;

    /** Default broker user name (empty — populated per environment). */
    public static final String DEFAULT_USER_NAME = "";

    /** Default broker password (empty — populated per environment, never logged). */
    public static final String DEFAULT_PASSWORD = "";

    /** Default Queue Communication Unit name. */
    public static final String DEFAULT_QCU_NAME = "QCU1";

    /** Default connection establishment timeout in seconds. */
    public static final int DEFAULT_CONN_TIME_SEC = 30;

    /** Default reply wait timeout in seconds. */
    public static final int DEFAULT_REPLY_TMOUT_SEC = 30;

    /** Default security exit flag (0 = disabled). */
    public static final int DEFAULT_SEC_EXIT_FLAG = 0;

    /** Default consumer polling interval in milliseconds. */
    public static final long DEFAULT_CONSUMER_POLL_INTERVAL_MS = 100L;

    /** Default admin endpoint host. */
    public static final String DEFAULT_ADMIN_HOST = "127.0.0.1";

    /** Default admin endpoint port. */
    public static final int DEFAULT_ADMIN_PORT = 9999;

    /** Broker hostname or IP address. */
    private String brokerHost = DEFAULT_BROKER_HOST;

    /** Broker TCP port number. */
    private int brokerPort = DEFAULT_BROKER_PORT;

    /** Numeric broker identifier (TLQ {@code brokerId}). */
    private int brokerId = DEFAULT_BROKER_ID;

    /** Broker user name (broker credential, not user account). */
    private String userName = DEFAULT_USER_NAME;

    /** Broker password (sensitive — never logged or persisted). */
    private String password = DEFAULT_PASSWORD;

    /** Queue Communication Unit (QCU) name. */
    private String qcuName = DEFAULT_QCU_NAME;

    /** Connection establishment timeout in seconds. */
    private int connTimeSec = DEFAULT_CONN_TIME_SEC;

    /** Reply wait timeout in seconds. */
    private int replyTmoutSec = DEFAULT_REPLY_TMOUT_SEC;

    /** Security exit flag (0 = disabled, 1 = enabled). */
    private int secExitFlag = DEFAULT_SEC_EXIT_FLAG;

    /** Consumer polling interval in milliseconds (long for high-throughput headroom). */
    private long consumerPollIntervalMs = DEFAULT_CONSUMER_POLL_INTERVAL_MS;

    /** Admin endpoint host. */
    private String adminHost = DEFAULT_ADMIN_HOST;

    /** Admin endpoint port. */
    private int adminPort = DEFAULT_ADMIN_PORT;

    /**
     * Return the broker host.
     *
     * @return the broker host, never {@code null}
     */
    public String getBrokerHost() {
        return brokerHost;
    }

    /**
     * Set the broker host.
     *
     * @param brokerHost the broker host to set
     */
    public void setBrokerHost(final String brokerHost) {
        this.brokerHost = brokerHost;
    }

    /**
     * Return the broker port.
     *
     * @return the broker port number
     */
    public int getBrokerPort() {
        return brokerPort;
    }

    /**
     * Set the broker port.
     *
     * @param brokerPort the broker port number to set
     */
    public void setBrokerPort(final int brokerPort) {
        this.brokerPort = brokerPort;
    }

    /**
     * Return the broker numeric identifier.
     *
     * @return the broker id
     */
    public int getBrokerId() {
        return brokerId;
    }

    /**
     * Set the broker numeric identifier.
     *
     * @param brokerId the broker id to set
     */
    public void setBrokerId(final int brokerId) {
        this.brokerId = brokerId;
    }

    /**
     * Return the broker user name.
     *
     * @return the broker user name, may be empty
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the broker user name.
     *
     * @param userName the broker user name to set
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Return the broker password.
     *
     * <p>Sensitive value — callers must never log or persist this directly.</p>
     *
     * @return the broker password, may be empty
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the broker password.
     *
     * @param password the broker password to set
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Return the Queue Communication Unit name.
     *
     * @return the QCU name, never {@code null}
     */
    public String getQcuName() {
        return qcuName;
    }

    /**
     * Set the Queue Communication Unit name.
     *
     * @param qcuName the QCU name to set
     */
    public void setQcuName(final String qcuName) {
        this.qcuName = qcuName;
    }

    /**
     * Return the connection establishment timeout in seconds.
     *
     * @return the connection timeout in seconds
     */
    public int getConnTimeSec() {
        return connTimeSec;
    }

    /**
     * Set the connection establishment timeout in seconds.
     *
     * @param connTimeSec the connection timeout to set
     */
    public void setConnTimeSec(final int connTimeSec) {
        this.connTimeSec = connTimeSec;
    }

    /**
     * Return the reply wait timeout in seconds.
     *
     * @return the reply timeout in seconds
     */
    public int getReplyTmoutSec() {
        return replyTmoutSec;
    }

    /**
     * Set the reply wait timeout in seconds.
     *
     * @param replyTmoutSec the reply timeout to set
     */
    public void setReplyTmoutSec(final int replyTmoutSec) {
        this.replyTmoutSec = replyTmoutSec;
    }

    /**
     * Return the security exit flag.
     *
     * @return the security exit flag (0 = disabled, 1 = enabled)
     */
    public int getSecExitFlag() {
        return secExitFlag;
    }

    /**
     * Set the security exit flag.
     *
     * @param secExitFlag the security exit flag to set
     */
    public void setSecExitFlag(final int secExitFlag) {
        this.secExitFlag = secExitFlag;
    }

    /**
     * Return the consumer polling interval in milliseconds.
     *
     * @return the polling interval in milliseconds
     */
    public long getConsumerPollIntervalMs() {
        return consumerPollIntervalMs;
    }

    /**
     * Set the consumer polling interval in milliseconds.
     *
     * @param consumerPollIntervalMs the polling interval to set
     */
    public void setConsumerPollIntervalMs(final long consumerPollIntervalMs) {
        this.consumerPollIntervalMs = consumerPollIntervalMs;
    }

    /**
     * Return the admin endpoint host.
     *
     * @return the admin host, never {@code null}
     */
    public String getAdminHost() {
        return adminHost;
    }

    /**
     * Set the admin endpoint host.
     *
     * @param adminHost the admin host to set
     */
    public void setAdminHost(final String adminHost) {
        this.adminHost = adminHost;
    }

    /**
     * Return the admin endpoint port.
     *
     * @return the admin port number
     */
    public int getAdminPort() {
        return adminPort;
    }

    /**
     * Set the admin endpoint port.
     *
     * @param adminPort the admin port number to set
     */
    public void setAdminPort(final int adminPort) {
        this.adminPort = adminPort;
    }
}
