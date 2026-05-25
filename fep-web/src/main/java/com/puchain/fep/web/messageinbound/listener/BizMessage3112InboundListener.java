package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 3112 inbound consumer — FR-MSG-3112 银行被动接收（PRD §4.6 line 841 +
 * §4.7 line 862 模式5）。
 *
 * <p>模式5 流程: HNDEMP→FEP（银行角色）推送 3112 核心企业授信查询请求 →
 * FEP 先返回 9120 应答 → 异步返回 3113 回执。本 listener 实现 <b>Phase 1：先返
 * 9120 应答 + 持久化 inbound 记录</b>；3113 回执内容组装（查授信额度 + 构造
 * HxqyCreditAmt3113 + enqueue outbound）<b>deferred 至 roadmap Plan C</b>
 * （需行内授信查询接口规范）。</p>
 *
 * <p>结构复用 {@code BizMessage2101InboundListener}（模式6 先例）：listener 在
 * dispatcher {@code @Transactional} 边界内同步执行；9120 ack 经
 * {@link OutboundMessageEnqueuePort#submit} 走 {@code REQUIRES_NEW} 独立提交，
 * 跨 tx 一致性由 idempotencyKey + at-least-once 兜底。</p>
 *
 * <p><b>与 2101 的差异</b>:</p>
 * <ul>
 *   <li>幂等 key 前缀 {@code ACK-9120-3112-}（按源报文类型命名空间隔离，避免与
 *       2101/Plan B 其他 9120-ack listener 在共享 serialNo 空间碰撞）。</li>
 *   <li>{@code HxqyCreditAmt3112} 携带真实业务 SerialNo（dispatcher.extractSerialNo
 *       返回业务 serialNo 而非 transitionNo fallback）。</li>
 * </ul>
 *
 * <p>容错策略（同 2101）: record dup(BIZ_5002)→WARN+继续 ack；body=null→跳过
 * record+仍发 ack(debug=BODY_NULL_OR_UNMARSHAL_SKIPPED)；ack DUP_KEY→WARN 静默；
 * 非 3112 event→早返回；type mismatch→IllegalStateException 透传；institutionCode
 * 空→FepBusinessException(COLLECT_ASSEMBLE_FAILURE)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage3112InboundListener {

    private static final Logger LOG = LoggerFactory.getLogger(BizMessage3112InboundListener.class);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final String PAYLOAD_DATA_TYPE = "ACK_9120";
    private static final String DEBUG_BODY_NULL = "BODY_NULL_OR_UNMARSHAL_SKIPPED";
    private static final String IDEMPOTENCY_KEY_PREFIX = "ACK-9120-3112-";
    private static final int IDEMPOTENCY_KEY_HEX_LEN = 32;
    private static final String MESSAGE_CODE = "3112";

    private final BizMessageRecordService recordService;
    private final OutboundMessageEnqueuePort enqueuePort;
    private final String institutionCode;
    private final Clock clock;

    /**
     * Spring constructor injection.
     *
     * @param recordService   message record service for inbound persist, non-null
     * @param enqueuePort     outbound enqueue port for 9120 ack delivery, non-null
     * @param institutionCode 14-digit sending institution code from
     *                        {@code fep.collector.institution-code} property
     * @param clock           clock for deriving Asia/Shanghai entrustDate, non-null
     */
    public BizMessage3112InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            @Value("${fep.collector.institution-code:}") final String institutionCode,
            final Clock clock) {
        this.recordService = Objects.requireNonNull(recordService, "recordService");
        this.enqueuePort = Objects.requireNonNull(enqueuePort, "enqueuePort");
        this.institutionCode = institutionCode == null ? "" : institutionCode;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Synchronous {@code @EventListener} fired inside the dispatcher's
     * {@code @Transactional} boundary.
     *
     * @param event the inbound-processed event, non-null
     * @throws IllegalStateException when the registered body POJO does not match
     *                               {@link HxqyCreditAmt3112} (registry contract violation,
     *                               forces dispatcher rollback)
     * @throws FepBusinessException  when institutionCode is blank
     *                               ({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE}),
     *                               or when record persist surfaces a non-BIZ_5002 business error,
     *                               or when ack enqueue surfaces a non-COLLECT_DUPLICATE_KEY error
     */
    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "all log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    public void onProcessed(final InboundMessageProcessedEvent event) {
        if (event.type() != MessageType.MSG_3112) {
            return;
        }
        final String safeSerial = LogSanitizer.sanitize(event.serialNo());
        final HxqyCreditAmt3112 body = event.bodyAs(HxqyCreditAmt3112.class);
        String debugReason = null;
        if (body == null) {
            debugReason = DEBUG_BODY_NULL;
            LOG.warn("3112 listener: body=null, skip record persist; still enqueue 9120 ack "
                            + "serialNo={} debug={}",
                    LogSanitizer.sanitize(safeSerial), LogSanitizer.sanitize(debugReason));
        } else {
            persistRecord(event, safeSerial);
        }
        enqueue9120Ack(event, debugReason, safeSerial);
        LOG.info("3112 processed serialNo={} body={}",
                LogSanitizer.sanitize(safeSerial),
                LogSanitizer.sanitize(body != null ? "decoded" : "null"));
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "log arg wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    private void persistRecord(final InboundMessageProcessedEvent event, final String safeSerial) {
        try {
            final RecordCreateRequest req = new RecordCreateRequest();
            req.setMessageCode(MESSAGE_CODE);
            req.setSerialNo(event.serialNo());
            req.setDirection(MessageDirection.INBOUND);
            recordService.create(req);
        } catch (final FepBusinessException dup) {
            if (dup.getErrorCode() == FepErrorCode.BIZ_5002) {
                LOG.warn("3112 listener: record dup serialNo={}, continue 9120 ack (idempotent)",
                        LogSanitizer.sanitize(safeSerial));
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
        final String idempotencyKey = deriveAckIdempotencyKey(event.serialNo());
        final OutboundMessageEnvelope envelope = new OutboundMessageEnvelope(
                MessageType.MSG_9120.msgNo(),
                Direction.OUTBOUND,
                idempotencyKey,
                head,
                ack,
                PAYLOAD_DATA_TYPE,
                MESSAGE_CODE + "-serialNo:" + event.serialNo());
        try {
            enqueuePort.submit(envelope);
        } catch (final FepBusinessException dup) {
            if (dup.getErrorCode() == FepErrorCode.COLLECT_DUPLICATE_KEY) {
                LOG.warn("9120 ack already enqueued for serialNo={} "
                                + "(at-least-once idempotency hit), swallow dup",
                        LogSanitizer.sanitize(safeSerial));
                return;
            }
            throw dup;
        }
    }

    /**
     * Deterministic 32-hex idempotency key derived from serialNo (3112 namespace
     * prefix) so retransmissions of the same 3112 collapse to the same ack
     * envelope row via the {@code uk_outbound_queue_idempotency_key} UNIQUE
     * constraint, without colliding with other inbound 9120-ack producers.
     *
     * @param serialNo the inbound 3112 business serial number, non-null
     * @return 32-char hex string suitable for {@code OutboundMessageEnvelope.idempotencyKey}
     */
    private static String deriveAckIdempotencyKey(final String serialNo) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha256.digest(
                    (IDEMPOTENCY_KEY_PREFIX + serialNo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, IDEMPOTENCY_KEY_HEX_LEN);
        } catch (final NoSuchAlgorithmException nsa) {
            throw new IllegalStateException("SHA-256 algorithm missing on JVM", nsa);
        }
    }
}
