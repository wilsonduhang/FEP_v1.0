package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.xml.XmlCodec;
import com.puchain.fep.transport.api.MessageListener;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * TLQ inbound message listener — bridges {@link com.puchain.fep.transport.api.TlqConsumer}
 * push-mode delivery into {@link InboundMessageDispatcher}.
 *
 * <p>P3 Task 3 — message-driven wiring (PRD §3.1.1 + §3.1.2). Subscribed to
 * {@link com.puchain.fep.transport.model.TlqChannel#REALTIME_RECEIVE} and
 * {@link com.puchain.fep.transport.model.TlqChannel#BATCH_RECEIVE} via
 * {@link com.puchain.fep.web.messageinbound.config.TlqInboundConfiguration}.</p>
 *
 * <h3>Failure handling — silent-failure (P3-DEFER-DLQ)</h3>
 * <p>The listener catches every exception that escapes {@code dispatcher.dispatch}
 * and only logs an ERROR entry. The broker treats the delivery as acknowledged
 * even if the message could not be parsed or persisted. This is a deliberate
 * Phase 2 simplification (Plan v1a P1-Q2): a real DLQ + retry policy is
 * deferred to ticket {@code P3-DEFER-DLQ} and tracked separately.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TlqInboundListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(TlqInboundListener.class);

    /**
     * Fallback transitionNo length — last 8 digits of msgId when the business
     * head TransitionNo is unavailable.
     */
    private static final int TRANSITION_NO_LEN = 8;

    private final InboundMessageDispatcher dispatcher;
    private final XmlCodec xmlCodec;

    /**
     * Spring constructor injection.
     *
     * @param dispatcher the inbound message dispatcher, non-null
     * @param xmlCodec   the shared XML codec, non-null
     */
    public TlqInboundListener(final InboundMessageDispatcher dispatcher,
                               final XmlCodec xmlCodec) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.xmlCodec = Objects.requireNonNull(xmlCodec, "xmlCodec");
    }

    /**
     * Receive a TLQ message, parse the CFX header, and dispatch.
     *
     * <p>All failures are caught and logged — the broker delivery is treated
     * as acknowledged regardless of downstream pipeline outcome. See class
     * Javadoc for the silent-failure rationale and the {@code P3-DEFER-DLQ}
     * follow-up ticket.</p>
     *
     * @param message the TLQ message, non-null (delivered by the consumer)
     */
    @Override
    public void onMessage(final TlqMessage message) {
        if (message == null) {
            // defensive: contract says non-null, but mocks may surface null
            LOG.warn("TLQ inbound message is null, ignored");
            return;
        }
        try {
            final String payload = message.getPayload();
            final byte[] xml = payload.getBytes(StandardCharsets.UTF_8);
            final CfxMessage cfx = xmlCodec.unmarshal(payload);
            final CommonHead head = cfx.getHead();
            if (head == null) {
                LOG.error("TLQ inbound rejected: CFX head is null msgId={}",
                        LogSanitizer.sanitize(message.getMsgId()));
                return;
            }
            final String messageType = head.getMsgNo();
            final String transitionNo = InboundTransitionNoExtractor.extract(payload)
                    .orElseGet(() -> deriveTransitionNo(head.getMsgId()));
            if (messageType == null || transitionNo == null) {
                LOG.error("TLQ inbound rejected: missing msgNo/msgId msgId={} msgNo={}",
                        LogSanitizer.sanitize(message.getMsgId()),
                        LogSanitizer.sanitize(messageType));
                return;
            }
            dispatcher.dispatch(messageType, transitionNo, xml);
            LOG.info("TLQ inbound dispatched msgNo={} transitionNo={}",
                    LogSanitizer.sanitize(messageType),
                    LogSanitizer.sanitize(transitionNo));
        } catch (RuntimeException e) {
            // P3-DEFER-DLQ: silent-failure — log + ack without retry/DLQ.
            // Plan v1a P1-Q2 explicitly defers a proper DLQ policy.
            LOG.error("TLQ inbound listener failed msgId={} cause={}",
                    LogSanitizer.sanitize(message.getMsgId()),
                    LogSanitizer.sanitize(e.getMessage()),
                    e);
        }
    }

    /**
     * Fallback transitionNo derivation — last {@value #TRANSITION_NO_LEN}
     * characters of the CFX {@code msgId}.
     *
     * <p>R3 升级后此为**兜底**路径：仅当 {@link InboundTransitionNoExtractor}
     * 无法从业务头（BatchHead/RealHead）提取真实 {@code TransitionNo}（XML
     * 异常 / 缺失字段）时使用。正常路径取业务头真值（PRD §3.2.3/§3.2.4
     * 「按原值回填」）。历史占位语义见 ADR
     * {@code 2026-05-05-inbound-realhead-extraction-blocked.md} §R3 Addendum。</p>
     *
     * @param msgId the CFX msgId, may be {@code null}
     * @return derived transitionNo or {@code null} when {@code msgId} cannot
     *         supply at least {@link #TRANSITION_NO_LEN} characters
     */
    private static String deriveTransitionNo(final String msgId) {
        if (msgId == null || msgId.length() < TRANSITION_NO_LEN) {
            return null;
        }
        return msgId.substring(msgId.length() - TRANSITION_NO_LEN);
    }
}
