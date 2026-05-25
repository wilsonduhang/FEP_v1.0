package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * inbound 9120-ack 受理 listener 模板基类 — 持久化 INBOUND 记录 + 强制返回 9120 应答。
 *
 * <p>封装 6 个同型受理 listener（2101 模式6 / 3112+3105+3009+3103+3113 模式2/3/5 受理侧
 * 参照模式6 全返 9120，muzhou 2026-05-23 Q1 决策）的共享逻辑。子类仅提供 {@link #messageType()}
 * / {@link #messageCode()} / {@link #bodyClass()} 三钩子 + {@code @Component} 注解。</p>
 *
 * <p>listener 在 dispatcher {@code @Transactional} 边界内同步执行；9120 ack 经
 * {@link OutboundMessageEnqueuePort#submit} 走 {@code REQUIRES_NEW} 独立提交，跨 tx
 * 一致性由 {@link AckIdempotencyKeys} 命名空间幂等 key + at-least-once 兜底。</p>
 *
 * <p>容错: record dup(BIZ_5002)→WARN+继续 ack；body=null→跳过 record+仍发 ack(debug)；
 * ack DUP_KEY→WARN 静默；非本类 type→早返回；type mismatch→IllegalStateException 透传；
 * institutionCode 空→FepBusinessException(COLLECT_ASSEMBLE_FAILURE)。</p>
 *
 * <p><b>@EventListener 继承</b>: 本基类非 {@code @Component}；各 {@code @Component} 子类
 * bean 继承本 {@code @EventListener} 方法各自注册，按 {@link #messageType()} guard。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class AbstractAck9120InboundListener {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAck9120InboundListener.class);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final String PAYLOAD_DATA_TYPE = "ACK_9120";
    private static final String DEBUG_BODY_NULL = "BODY_NULL_OR_UNMARSHAL_SKIPPED";

    private final BizMessageRecordService recordService;
    private final OutboundMessageEnqueuePort enqueuePort;
    private final String institutionCode;
    private final Clock clock;

    /**
     * Subclass constructor injection.
     *
     * @param recordService   message record service, non-null
     * @param enqueuePort     outbound enqueue port for 9120 ack, non-null
     * @param institutionCode 14-digit institution code from {@code fep.collector.institution-code}
     * @param clock           clock for Asia/Shanghai entrustDate, non-null
     */
    protected AbstractAck9120InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            final String institutionCode,
            final Clock clock) {
        this.recordService = Objects.requireNonNull(recordService, "recordService");
        this.enqueuePort = Objects.requireNonNull(enqueuePort, "enqueuePort");
        this.institutionCode = institutionCode == null ? "" : institutionCode;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** @return the inbound message type this listener handles, non-null */
    protected abstract MessageType messageType();

    /** @return the 4-digit message code (e.g. {@code "3105"}) for record + ack namespace, non-null */
    protected abstract String messageCode();

    /** @return the registered body POJO class for type-mismatch guard, non-null */
    protected abstract Class<?> bodyClass();

    /**
     * Synchronous {@code @EventListener} inside dispatcher {@code @Transactional} boundary.
     *
     * @param event the inbound-processed event, non-null
     * @throws IllegalStateException when body POJO mismatches {@link #bodyClass()}
     * @throws FepBusinessException  blank institutionCode / non-BIZ_5002 persist error /
     *                               non-COLLECT_DUPLICATE_KEY ack error
     */
    // Inherited @EventListener: Spring 6.2 EventListenerMethodProcessor scans inherited
    // public @EventListener methods and registers ONE adapter per concrete @Component
    // subclass bean (not a single shared registration). Each bean's invocation guards
    // on messageType() and early-returns for foreign types — identical to standalone-
    // listener semantics. Do not move this annotation to subclasses.
    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "all log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    public void onProcessed(final InboundMessageProcessedEvent event) {
        if (event.type() != messageType()) {
            return;
        }
        final String safeSerial = LogSanitizer.sanitize(event.serialNo());
        final Object body = event.bodyAs(bodyClass());
        String debugReason = null;
        if (body == null) {
            debugReason = DEBUG_BODY_NULL;
            LOG.warn("{} listener: body=null, skip record persist; still enqueue 9120 ack "
                            + "serialNo={} debug={}",
                    LogSanitizer.sanitize(messageCode()),
                    LogSanitizer.sanitize(safeSerial), LogSanitizer.sanitize(debugReason));
        } else {
            persistRecord(event, safeSerial);
        }
        enqueue9120Ack(event, debugReason, safeSerial);
        LOG.info("{} processed serialNo={} body={}",
                LogSanitizer.sanitize(messageCode()),
                LogSanitizer.sanitize(safeSerial),
                LogSanitizer.sanitize(body != null ? "decoded" : "null"));
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "log arg wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    private void persistRecord(final InboundMessageProcessedEvent event, final String safeSerial) {
        try {
            final RecordCreateRequest req = new RecordCreateRequest();
            req.setMessageCode(messageCode());
            req.setSerialNo(event.serialNo());
            req.setDirection(MessageDirection.INBOUND);
            recordService.create(req);
        } catch (final FepBusinessException dup) {
            if (dup.getErrorCode() == FepErrorCode.BIZ_5002) {
                LOG.warn("{} listener: record dup serialNo={}, continue 9120 ack (idempotent)",
                        LogSanitizer.sanitize(messageCode()), LogSanitizer.sanitize(safeSerial));
            } else {
                throw dup;
            }
        }
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "log arg wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    private void enqueue9120Ack(final InboundMessageProcessedEvent event,
                                final String debugReason,
                                final String safeSerial) {
        if (institutionCode.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing fep.collector.institution-code; cannot build 9120 ack for serialNo="
                            + safeSerial);
        }
        final MsgReturn9120 ack = new MsgReturn9120();
        ack.setOriMsgNo(event.serialNo());
        if (debugReason != null) {
            ack.setDebug(debugReason);
        }
        final String entrustDate = YYYYMMDD.format(LocalDate.now(clock.withZone(BEIJING)));
        final OutboundHeadFields head = new OutboundHeadFields(
                institutionCode, entrustDate, event.transitionNo());
        final String idempotencyKey = AckIdempotencyKeys.derive(messageCode(), event.serialNo());
        final OutboundMessageEnvelope envelope = new OutboundMessageEnvelope(
                MessageType.MSG_9120.msgNo(),
                Direction.OUTBOUND,
                idempotencyKey,
                head,
                ack,
                PAYLOAD_DATA_TYPE,
                messageCode() + "-serialNo:" + event.serialNo());
        try {
            enqueuePort.submit(envelope);
        } catch (final FepBusinessException dup) {
            if (dup.getErrorCode() == FepErrorCode.COLLECT_DUPLICATE_KEY) {
                LOG.warn("9120 ack already enqueued for {} serialNo={} "
                                + "(at-least-once idempotency hit), swallow dup",
                        LogSanitizer.sanitize(messageCode()), LogSanitizer.sanitize(safeSerial));
                return;
            }
            throw dup;
        }
    }
}
