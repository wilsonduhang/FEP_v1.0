package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.api.TlqConnectionFactory;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqConnContext;
import com.tongtech.tlq.base.TlqConnection;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqQCU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Production {@link TlqConnectionFactory} implementation backed by the Tongtech
 * TongLINK/Q SDK ({@code com.tongtech.tlq.base.*}).
 *
 * <p>Manages the lifecycle of a single {@link TlqConnection} and the QCU handle
 * ({@link TlqQCU}) it exposes. The QCU is the unit through which the producer,
 * consumer, and admin adapters (delivered in P1c Tasks 5/6/7) place and receive
 * messages — package-private accessors {@link #getQCU()} and
 * {@link #getConnection()} surface the SDK objects to those collaborators while
 * keeping them invisible to the rest of the codebase.</p>
 *
 * <p><b>v1a notes</b> (verified by {@code javap} on the SDK classes):</p>
 * <ul>
 *   <li>{@link TlqConnContext} fields are all-public; the eight
 *       {@code TongtechTlqProperties} fields are direct-assigned (no setters).
 *       The remaining {@link TlqConnContext} fields are intentionally left at
 *       their SDK default values until P1c-IT-bridge real-machine validation
 *       calibrates them (Plan §Risk Register R8/R9).</li>
 *   <li>{@code connect()} failure is mapped to {@link FepErrorCode#TRANS_7002}
 *       directly (verifying validation criterion #4 in Plan §T4) rather than
 *       routing through {@link TongtechErrorMapper}, whose default branch is
 *       {@code TRANS_7003}. The mapper is reserved for send/receive/ack/admin
 *       paths added by Tasks 5/6/7.</li>
 *   <li>{@code disconnect()} is idempotent: it nulls out references after the
 *       first close so a second call is a no-op. SDK exceptions during close
 *       are demoted to WARN logs (we cannot recover, but we must not propagate
 *       them so the caller can move on with shutdown).</li>
 * </ul>
 *
 * <p>Registered as a Spring {@link Component} discovered by
 * {@code TongtechTransportConfiguration}'s component scan, which is itself
 * gated by {@code fep.transport.provider=tongtech}; the mock provider path
 * keeps {@code InMemoryTlqConnectionFactory} active.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechTlqConnectionFactory implements TlqConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechTlqConnectionFactory.class);

    private final TongtechTlqProperties properties;
    private final TongtechErrorMapper errorMapper;

    private volatile boolean connected;
    // volatile so getQCU()/getConnection() lock-free hot-path reads see writes
    // performed under the {@code synchronized} connect()/disconnect() critical sections.
    private volatile TlqConnection connection;
    private volatile TlqQCU qcu;

    /**
     * Construct the factory with its required collaborators.
     *
     * @param properties  bound {@code fep.transport.tongtech.*} configuration
     * @param errorMapper SDK exception → {@link FepBusinessException} mapper
     *                    (used by Tasks 5/6/7 send/receive/admin paths;
     *                    {@code connect()} maps directly to {@code TRANS_7002})
     */
    public TongtechTlqConnectionFactory(
            final TongtechTlqProperties properties,
            final TongtechErrorMapper errorMapper) {
        this.properties = properties;
        this.errorMapper = errorMapper;
    }

    /**
     * Establish a TLQ connection and open the configured QCU.
     *
     * <p>This method is {@code synchronized} to serialize concurrent connect
     * calls (the SDK's {@link TlqConnection} is not documented as
     * thread-safe).</p>
     *
     * @throws FepBusinessException with {@link FepErrorCode#TRANS_7002} when
     *         the SDK fails to establish the connection or open the QCU
     */
    @Override
    public synchronized void connect() {
        if (connected) {
            LOG.debug("Tongtech TLQ already connected; ignoring redundant connect()");
            return;
        }
        try {
            TlqConnContext ctx = new TlqConnContext();
            ctx.HostName = properties.getBrokerHost();
            ctx.ListenPort = properties.getBrokerPort();
            ctx.BrokerId = properties.getBrokerId();
            ctx.ConnTime = properties.getConnTimeSec();
            ctx.ReplyTmout = properties.getReplyTmoutSec();
            ctx.UserName = properties.getUserName();
            ctx.Password = properties.getPassword();
            ctx.SecExitFlag = properties.getSecExitFlag();

            connection = new TlqConnection(ctx);
            qcu = connection.openQCU(properties.getQcuName());
            connected = true;
            LOG.info("Tongtech TLQ connected: brokerHost={} brokerPort={} qcu={}",
                    properties.getBrokerHost(),
                    properties.getBrokerPort(),
                    properties.getQcuName());
        } catch (TlqException e) {
            connected = false;
            connection = null;
            qcu = null;
            // Validation criterion #4: connect() failure must map to TRANS_7002
            // regardless of cause-keyword heuristics.
            throw new FepBusinessException(
                    FepErrorCode.TRANS_7002,
                    FepErrorCode.TRANS_7002.getDefaultMessage() + ": " + e.getErrorCause(),
                    e);
        }
    }

    /**
     * Close the QCU and the underlying connection. Safe to call multiple times.
     *
     * <p>SDK exceptions during close are caught and demoted to WARN logs; the
     * method always completes normally so callers can proceed with their own
     * shutdown sequence.</p>
     */
    @Override
    public synchronized void disconnect() {
        if (qcu != null) {
            try {
                qcu.close();
            } catch (TlqException e) {
                LOG.warn("Tongtech TLQ QCU close failed (ignored): cause={}", e.getErrorCause());
            }
            qcu = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (TlqException e) {
                LOG.warn("Tongtech TLQ connection close failed (ignored): cause={}",
                        e.getErrorCause());
            }
            connection = null;
        }
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Return the live {@link TlqQCU} for use by sibling adapters
     * ({@code TongtechTlqProducer}, {@code TongtechTlqConsumer},
     * etc., delivered in Tasks 5/6/7).
     *
     * @return the QCU handle, never {@code null}
     * @throws FepBusinessException with {@link FepErrorCode#TRANS_7002} if
     *         {@link #connect()} has not been invoked or has been
     *         {@link #disconnect()}ed
     */
    TlqQCU getQCU() {
        if (qcu == null) {
            throw new FepBusinessException(
                    FepErrorCode.TRANS_7002,
                    "TLQ QCU is not open; connect() must be called first");
        }
        return qcu;
    }

    /**
     * Return the live {@link TlqConnection} for use by sibling adapters that
     * need direct access (e.g. admin endpoint operations in Task 7).
     *
     * @return the SDK connection, never {@code null}
     * @throws FepBusinessException with {@link FepErrorCode#TRANS_7002} if
     *         {@link #connect()} has not been invoked or has been
     *         {@link #disconnect()}ed
     */
    TlqConnection getConnection() {
        if (connection == null) {
            throw new FepBusinessException(
                    FepErrorCode.TRANS_7002,
                    "TLQ connection is not open; connect() must be called first");
        }
        return connection;
    }

    /**
     * Return the {@link TongtechErrorMapper} injected at construction so sibling
     * adapters in the same package (Tasks 5/6/7 producer/consumer/admin) can
     * reuse the single mapper instance bound to this factory.
     *
     * @return the configured error mapper, never {@code null}
     */
    TongtechErrorMapper getErrorMapper() {
        return errorMapper;
    }
}
