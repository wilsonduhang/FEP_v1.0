package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqMsgOpt;
import com.tongtech.tlq.base.TlqQCU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Production {@link TlqProducer} backed by the Tongtech TongLINK/Q SDK
 * ({@code com.tongtech.tlq.base.*}).
 *
 * <p><b>v1b key design</b> (Plan §Task 5):</p>
 * <ul>
 *   <li><b>No {@code @Component}</b> — this class is registered exclusively via
 *       {@link com.puchain.fep.transport.tongtech.config.TongtechProducerConfig#tongtechTlqProducer}
 *       so the {@link com.puchain.fep.transport.api.RetryableProducer} wrapper bean does not
 *       collide with a duplicate underlying producer registration.</li>
 *   <li><b>{@code putMessage} failure returns {@link SendResult#fail}</b> — the
 *       {@code RetryableProducer} decorator is contract-bound to inspect
 *       {@link SendResult#success()} / {@link SendResult#error()} to drive its
 *       exponential-backoff retry and dead-letter routing (see
 *       {@code RetryableProducer.send:71-102}). Throwing here would short-circuit
 *       both layers.</li>
 *   <li><b>Connection failures still propagate</b> — when {@link TongtechTlqConnectionFactory#connect()}
 *       fails it raises {@link com.puchain.fep.common.exception.FepBusinessException}
 *       with {@link FepErrorCode#TRANS_7002}; we do not swallow this since
 *       broker-down conditions cannot succeed via retry at the producer layer
 *       and must surface to the caller.</li>
 * </ul>
 *
 * <p>Refer to PRD §3.1 (TLQ Producer initialisation) and §3.6 (large-message split,
 * delegated to {@link TongtechMessageMapper#toSdkMessage}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TongtechTlqProducer implements TlqProducer {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechTlqProducer.class);

    private final TongtechTlqConnectionFactory factory;
    private final TongtechMessageMapper mapper;
    private final QueueNameResolver resolver;
    private final TongtechChannelMapper channelMapper;
    private final TongtechErrorMapper errorMapper;

    /**
     * Construct the producer with its required collaborators.
     *
     * @param factory       the live SDK connection factory (non-null)
     * @param mapper        FEP ↔ SDK {@code TlqMessage} mapper (non-null)
     * @param resolver      queue name resolver bound to the institution (non-null)
     * @param channelMapper {@link TlqMessage} channel → queue type mapper (non-null)
     * @param errorMapper   SDK exception → {@link FepErrorCode} mapper (non-null)
     */
    public TongtechTlqProducer(final TongtechTlqConnectionFactory factory,
                               final TongtechMessageMapper mapper,
                               final QueueNameResolver resolver,
                               final TongtechChannelMapper channelMapper,
                               final TongtechErrorMapper errorMapper) {
        this.factory = factory;
        this.mapper = mapper;
        this.resolver = resolver;
        this.channelMapper = channelMapper;
        this.errorMapper = errorMapper;
    }

    /**
     * Send a FEP {@link TlqMessage} via the underlying SDK QCU using a
     * begin/put/commit transaction. Lazy-connects on first call.
     *
     * @param message the FEP message to send (non-null)
     * @return {@link SendResult#ok} on success; {@link SendResult#fail} with
     *         {@code "<TRANS_70xx>:<cause>"} as error detail when the SDK raises
     *         a {@link TlqException}
     * @throws com.puchain.fep.common.exception.FepBusinessException
     *         {@link FepErrorCode#TRANS_7002} when the underlying connection
     *         cannot be established (not retried at the producer layer)
     */
    @Override
    public SendResult send(final TlqMessage message) {
        if (!factory.isConnected()) {
            // Connection-level failure surfaces as FepBusinessException(TRANS_7002)
            // and is intentionally NOT mapped to SendResult.fail — broker-down
            // conditions cannot succeed via retry within RetryableProducer's budget.
            factory.connect();
        }
        final TlqQCU qcu = factory.getQCU();

        final com.tongtech.tlq.base.TlqMessage sdkMsg = mapper.toSdkMessage(message);

        final TlqMsgOpt opt = new TlqMsgOpt();
        opt.QueName = resolver.resolve(channelMapper.toQueueType(message.getChannel()));

        try {
            qcu.txBegin();
            qcu.putMessage(sdkMsg, opt);
            qcu.txCommit();
            LOG.debug("Sent msgId={} to queue={}", message.getMsgId(), opt.QueName);
            return SendResult.ok(message.getMsgId());
        } catch (TlqException e) {
            // Best-effort rollback; demote rollback failures to WARN so we never
            // mask the original error returned to RetryableProducer.
            try {
                qcu.txRollback();
            } catch (TlqException re) {
                LOG.warn("txRollback failed for msgId={}: {}",
                        message.getMsgId(), re.getErrorCause());
            }

            // v1b B-P0-1: return SendResult.fail (do NOT throw) so RetryableProducer
            // can drive retry + DLH routing based on result.success()/error().
            final FepErrorCode code = errorMapper.mapCause(e.getErrorCause());
            final String errorDetail = code.getCode() + ":" + e.getErrorCause();
            LOG.error("Send failed msgId={} code={} cause={}",
                    message.getMsgId(), code.getCode(), e.getErrorCause());
            return SendResult.fail(message.getMsgId(), errorDetail);
        }
    }
}
