package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.api.MessageListener;
import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqMsgOpt;
import com.tongtech.tlq.base.TlqQCU;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production {@link TlqConsumer} backed by the Tongtech TongLINK/Q SDK
 * ({@code com.tongtech.tlq.base.*}).
 *
 * <p><b>v1d Plan §Task 6 (lines 1654-1855) key design</b>:</p>
 * <ul>
 *   <li><b>Pull mode</b> ({@link #receive}): a single SDK
 *       {@code getMessage(TlqMessage, TlqMsgOpt)} call followed by an immediate
 *       {@code ackMessage(..., TLQACK_COMMIT)}. The {@link TlqMsgOpt#AckMode} is
 *       set to {@link TlqMsgOpt#TLQACK_USER} so that the SDK leaves the message
 *       in the consumer's pending state until we explicitly commit. Pull-mode
 *       callers receive a fully-mapped FEP {@link TlqMessage}; the SDK message is
 *       acked even on listener-less reads (pull-mode is the application's signal
 *       that downstream processing is the caller's responsibility).</li>
 *   <li><b>Push mode</b> ({@link #subscribe}): a daemon
 *       {@link ScheduledExecutorService} polls the queue at
 *       {@link TongtechTlqProperties#getConsumerPollIntervalMs} cadence. Each
 *       poll invokes {@link #pollAndDispatch} which:
 *       <ol>
 *         <li>polls a single message (no ack);</li>
 *         <li>if the {@link MessageDeduplicator} sees the {@code msgId} again,
 *             acks the SDK message (drop) without dispatching;</li>
 *         <li>otherwise dispatches to the {@link MessageListener} and acks on
 *             listener success;</li>
 *         <li>if the listener throws, <em>does not</em> ack so the broker
 *             redelivers (or routes to DLQ after retry exhaustion).</li>
 *       </ol>
 *       Subscriptions are <b>idempotent per channel</b> — calling
 *       {@code subscribe} twice on the same {@link TlqChannel} returns the same
 *       active task.</li>
 *   <li><b>Acknowledgement strategy</b>: {@link TlqMsgOpt#TLQACK_COMMIT} is the
 *       SDK constant for "commit the dequeue". Failures during ack are demoted
 *       to WARN (not propagated) — the broker will redeliver, which is safe and
 *       absorbed by {@link MessageDeduplicator}.</li>
 *   <li><b>Lifecycle</b>: {@link #shutdown} is invoked via {@link PreDestroy} on
 *       Spring container shutdown; cancels every active future and stops the
 *       scheduler.</li>
 * </ul>
 *
 * <p>Registered as a Spring {@link Component} discovered by
 * {@code TongtechTransportConfiguration}'s component scan, which is itself gated
 * by {@code fep.transport.provider=tongtech}; the mock provider path keeps
 * {@code InMemoryTlqConsumer} active.</p>
 *
 * <p>Refer to PRD §3.1 (TLQ Consumer initialisation), §3.6 (large-message split
 * — handled by {@link TongtechMessageMapper#fromSdkMessage}), and the FR-COMM
 * acceptance criteria for FR-COMM-TLQ-CONS / FR-COMM-TLQ-DEDUP.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechTlqConsumer implements TlqConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechTlqConsumer.class);

    /** Two daemon threads — one for the actively-scheduled poll, one for headroom. */
    private static final int SCHEDULER_THREADS = 2;

    private final TongtechTlqConnectionFactory factory;
    private final TongtechMessageMapper mapper;
    private final QueueNameResolver resolver;
    private final TongtechChannelMapper channelMapper;
    private final MessageDeduplicator deduplicator;
    private final TongtechErrorMapper errorMapper;
    private final TongtechTlqProperties props;

    private final ScheduledExecutorService scheduler;
    private final Map<TlqChannel, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Construct the consumer with its required collaborators.
     *
     * @param factory       the live SDK connection factory (non-null)
     * @param mapper        FEP ↔ SDK {@code TlqMessage} mapper (non-null)
     * @param resolver      queue name resolver bound to the institution (non-null)
     * @param channelMapper {@link TlqChannel} → queue type mapper (non-null)
     * @param deduplicator  message deduplicator for push-mode redelivery handling (non-null)
     * @param errorMapper   SDK exception → {@link com.puchain.fep.common.domain.FepErrorCode}
     *                      mapper (non-null)
     * @param props         transport properties supplying the polling cadence (non-null)
     */
    public TongtechTlqConsumer(final TongtechTlqConnectionFactory factory,
                               final TongtechMessageMapper mapper,
                               final QueueNameResolver resolver,
                               final TongtechChannelMapper channelMapper,
                               final MessageDeduplicator deduplicator,
                               final TongtechErrorMapper errorMapper,
                               final TongtechTlqProperties props) {
        this.factory = factory;
        this.mapper = mapper;
        this.resolver = resolver;
        this.channelMapper = channelMapper;
        this.deduplicator = deduplicator;
        this.errorMapper = errorMapper;
        this.props = props;
        this.scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREADS, new DaemonThreadFactory());
    }

    /**
     * Pull-mode receive: poll once for a message (lazy-connecting on the first
     * call) and acknowledge it immediately on success.
     *
     * @param channel the FEP channel to receive from (non-null)
     * @param timeout maximum wait window for a message (non-null)
     * @return the FEP {@link TlqMessage} if one was available within {@code timeout};
     *         {@link Optional#empty()} otherwise
     * @throws FepBusinessException with {@code TRANS_70xx} when the SDK raises a
     *         {@link TlqException} during the poll
     */
    @Override
    public Optional<TlqMessage> receive(final TlqChannel channel, final Duration timeout) {
        final Optional<RawSdkMessage> raw = receiveRawNoAck(channel, timeout);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        ackInternal(raw.get().sdkMsg(), raw.get().opt());
        return Optional.of(raw.get().fepMsg());
    }

    /**
     * Push-mode subscribe: register a {@link MessageListener} that the consumer
     * dispatches messages to via the daemon scheduler.
     *
     * <p>This method is <b>idempotent per channel</b> — calling {@code subscribe}
     * twice on the same {@link TlqChannel} keeps the original poll task and
     * does not register a second listener.</p>
     *
     * @param channel  the FEP channel to subscribe to (non-null)
     * @param listener the listener that consumes each non-duplicate message (non-null)
     */
    @Override
    public void subscribe(final TlqChannel channel, final MessageListener listener) {
        if (subscriptions.containsKey(channel)) {
            LOG.debug("Subscription for channel={} already active; subscribe() is a no-op", channel);
            return;
        }
        final long pollMs = props.getConsumerPollIntervalMs();
        final ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                () -> pollAndDispatch(channel, listener),
                0L, pollMs, TimeUnit.MILLISECONDS);
        subscriptions.put(channel, task);
        LOG.info("Subscribed to channel={} (poll interval {} ms)", channel, pollMs);
    }

    /**
     * Cancel the active subscription for {@code channel}, if any. Idempotent —
     * calling unsubscribe on a channel with no registered listener is a no-op.
     *
     * @param channel the FEP channel to unsubscribe (non-null)
     */
    @Override
    public void unsubscribe(final TlqChannel channel) {
        final ScheduledFuture<?> task = subscriptions.remove(channel);
        if (task != null) {
            task.cancel(true);
            LOG.info("Unsubscribed from channel={}", channel);
        }
    }

    /**
     * Cancel every active subscription and stop the scheduler. Invoked by the
     * Spring container during shutdown.
     */
    @PreDestroy
    public void shutdown() {
        subscriptions.values().forEach(task -> task.cancel(true));
        subscriptions.clear();
        scheduler.shutdownNow();
        LOG.info("TongtechTlqConsumer shut down (scheduler stopped, all subscriptions cancelled)");
    }

    /**
     * Single iteration of the push-mode poll loop, exposed package-private so
     * unit tests can drive dispatch behaviour deterministically without waiting
     * for the {@link ScheduledExecutorService} cadence.
     *
     * <p>Behaviour:</p>
     * <ol>
     *   <li>Poll a single message (no ack);</li>
     *   <li>If the deduplicator reports the {@code msgId} as a duplicate, ack
     *       the SDK message (drop) without dispatching;</li>
     *   <li>Otherwise dispatch to {@code listener} and ack on success;</li>
     *   <li>If the listener throws, log at ERROR and skip ack so the broker
     *       redelivers (or routes to DLQ after retry exhaustion).</li>
     * </ol>
     *
     * <p>Exceptions are deliberately not propagated — the surrounding
     * {@link ScheduledExecutorService} task would otherwise be cancelled on the
     * first error, ending the subscription loop.</p>
     *
     * @param channel  the channel being polled (non-null)
     * @param listener the listener to dispatch to (non-null)
     */
    void pollAndDispatch(final TlqChannel channel, final MessageListener listener) {
        try {
            final long pollMs = props.getConsumerPollIntervalMs();
            final Optional<RawSdkMessage> raw = receiveRawNoAck(channel, Duration.ofMillis(pollMs));
            if (raw.isEmpty()) {
                return;
            }
            final RawSdkMessage rawMsg = raw.get();
            final String msgId = rawMsg.fepMsg().getMsgId();
            if (deduplicator.isDuplicate(msgId)) {
                LOG.debug("Duplicate msgId={}, ack and drop", msgId);
                ackInternal(rawMsg.sdkMsg(), rawMsg.opt());
                return;
            }
            try {
                listener.onMessage(rawMsg.fepMsg());
                ackInternal(rawMsg.sdkMsg(), rawMsg.opt());
            } catch (RuntimeException ex) {
                // Listener-thrown exception: skip ack so broker redelivers / routes to DLQ.
                LOG.error("Listener failed for msgId={} channel={}; will redeliver",
                        msgId, channel, ex);
            }
        } catch (FepBusinessException ex) {
            // Mapped SDK exception — already logged by the mapper at debug; record
            // a single WARN here so the poll loop survives transient broker faults.
            LOG.warn("Poll iteration failed for channel={}: {}", channel, ex.getMessage());
        } catch (RuntimeException ex) {
            // Defensive: unexpected runtime errors must not cancel the schedule.
            LOG.error("Unexpected error in poll loop for channel={}", channel, ex);
        }
    }

    /**
     * Poll the SDK once for a message <em>without</em> acknowledging it. Lazy
     * connects on the first call. The returned {@link RawSdkMessage} keeps a
     * reference to both the SDK message (for ack) and the mapped FEP message
     * (for downstream dispatch).
     *
     * @param channel the FEP channel (non-null)
     * @param timeout the SDK wait window (non-null)
     * @return the raw SDK + FEP message pair if a message was available;
     *         {@link Optional#empty()} otherwise
     * @throws FepBusinessException when the SDK raises a {@link TlqException}
     */
    private Optional<RawSdkMessage> receiveRawNoAck(final TlqChannel channel, final Duration timeout) {
        if (!factory.isConnected()) {
            factory.connect();
        }
        final TlqQCU qcu = factory.getQCU();

        final TlqMsgOpt opt = new TlqMsgOpt();
        opt.QueName = resolver.resolve(channelMapper.toQueueType(channel));
        opt.WaitInterval = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        // TLQACK_USER → consumer must call ackMessage(...) explicitly; without
        // this, the SDK auto-acks on receive and we lose the redelivery safety net.
        opt.AckMode = (char) TlqMsgOpt.TLQACK_USER;

        final com.tongtech.tlq.base.TlqMessage sdkEmpty = new com.tongtech.tlq.base.TlqMessage();
        try {
            final int rc = qcu.getMessage(sdkEmpty, opt);
            if (rc != 0) {
                return Optional.empty();
            }
            final TlqMessage fep = mapper.fromSdkMessage(sdkEmpty, channel);
            return Optional.of(new RawSdkMessage(sdkEmpty, opt, fep));
        } catch (TlqException e) {
            LOG.error("getMessage failed for channel={} queue={}: {}",
                    channel, opt.QueName, e.getErrorCause());
            throw errorMapper.mapException(e);
        }
    }

    /**
     * Commit the SDK ack for a previously-polled message. Failures are demoted
     * to WARN — the broker will redeliver, which is absorbed by the
     * {@link MessageDeduplicator}.
     *
     * @param sdkMsg the SDK message previously returned by {@code getMessage}
     * @param opt    the same options object used during the poll
     */
    private void ackInternal(final com.tongtech.tlq.base.TlqMessage sdkMsg, final TlqMsgOpt opt) {
        try {
            factory.getQCU().ackMessage(sdkMsg, opt, TlqMsgOpt.TLQACK_COMMIT);
        } catch (TlqException e) {
            // Demote ack failure to WARN — broker will redeliver and the
            // MessageDeduplicator absorbs the duplicate on the next pass.
            LOG.warn("ackMessage failed for queue={}: {}", opt.QueName, e.getErrorCause());
        }
    }

    /**
     * Test-only accessor for the active subscriptions map. Package-private so
     * unit tests can verify idempotency and unsubscribe semantics without
     * waiting for the scheduler cadence.
     *
     * @return an unmodifiable view of the subscriptions map
     */
    Map<TlqChannel, ScheduledFuture<?>> subscriptionsForTest() {
        return java.util.Collections.unmodifiableMap(subscriptions);
    }

    /**
     * Test-only accessor for the scheduler. Package-private so unit tests can
     * assert {@link ScheduledExecutorService#isShutdown} after {@link #shutdown}.
     *
     * @return the underlying scheduler
     */
    ScheduledExecutorService schedulerForTest() {
        return scheduler;
    }

    /**
     * Tuple binding an SDK-level message to its options object and the
     * corresponding FEP-mapped message, so push-mode dispatch can ack the SDK
     * handle while delivering the FEP-shaped payload to listeners.
     */
    private record RawSdkMessage(
            com.tongtech.tlq.base.TlqMessage sdkMsg,
            TlqMsgOpt opt,
            TlqMessage fepMsg) {
    }

    /**
     * Daemon thread factory so the scheduler does not hold up JVM shutdown if
     * {@link #shutdown} is not invoked explicitly (defensive — Spring will
     * normally call {@code @PreDestroy}).
     */
    private static final class DaemonThreadFactory implements java.util.concurrent.ThreadFactory {

        private final AtomicLong counter = new AtomicLong(1L);

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(r, "tongtech-tlq-consumer-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
