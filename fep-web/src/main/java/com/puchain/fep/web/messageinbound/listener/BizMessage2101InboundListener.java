package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.batch.DataTransfer2101;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
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
 * 2101 inbound consumer — FR-MSG-2101（PRD §4.3 line 770 + §1.4 模式 6 line 863）。
 *
 * <p>模式 6 流程: HNDEMP→FEP 数据推送（2101）→ FEP 必须返回 9120 应答 +
 * 自行处理（无业务回执）。</p>
 *
 * <p>Listener 在 dispatcher {@code @Transactional} 边界内同步执行；9120 ack
 * 经 {@link OutboundMessageEnqueuePort#submit} 走 {@code @Transactional(REQUIRES_NEW)}
 * 独立提交（{@code JpaOutboundMessageEnqueueService} 强制 propagation）— 即 ack
 * 投递与 inbound 处理在不同事务，跨 tx 一致性通过 idempotencyKey + at-least-once
 * 兜底（重传 2101 的 ack 二次入队抛 COLLECT_DUPLICATE_KEY 被 listener 静默忽略）。</p>
 *
 * <p>容错策略:</p>
 * <ul>
 *   <li>record dup (BIZ_5002) → WARN + 继续 ack（接收端幂等）</li>
 *   <li>body=null → 跳过 record + 仍发 ack with debug=BODY_NULL_OR_UNMARSHAL_SKIPPED
 *       （PRD 模式 6 强制 ack）</li>
 *   <li>ack DUP_KEY (COLLECT_DUPLICATE_KEY) → WARN + 静默忽略（at-least-once）</li>
 *   <li>非 2101 event → 早返回</li>
 *   <li>type mismatch → IllegalStateException 透传（dispatcher rollback）</li>
 *   <li>institutionCode 空 → FepBusinessException(COLLECT_ASSEMBLE_FAILURE)</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage2101InboundListener {

    private static final Logger LOG = LoggerFactory.getLogger(BizMessage2101InboundListener.class);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final String PAYLOAD_DATA_TYPE = "ACK_9120";
    private static final String DEBUG_BODY_NULL = "BODY_NULL_OR_UNMARSHAL_SKIPPED";
    private static final String IDEMPOTENCY_KEY_PREFIX = "ACK-9120-";
    private static final int IDEMPOTENCY_KEY_HEX_LEN = 32;

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
    public BizMessage2101InboundListener(
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
     *                               {@link DataTransfer2101} (registry contract violation,
     *                               forces dispatcher rollback)
     * @throws FepBusinessException  when institutionCode is blank
     *                               ({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE}),
     *                               or when record persist surfaces a non-BIZ_5002 business error,
     *                               or when ack enqueue surfaces a non-COLLECT_DUPLICATE_KEY error
     */
    @EventListener
    public void onProcessed(final InboundMessageProcessedEvent event) {
        if (event.type() != MessageType.MSG_2101) {
            return;
        }
        final String safeSerial = LogSanitizer.sanitize(event.serialNo());
        final DataTransfer2101 body = event.bodyAs(DataTransfer2101.class);
        String debugReason = null;
        if (body == null) {
            debugReason = DEBUG_BODY_NULL;
            LOG.warn("2101 listener: body=null, skip record persist; still enqueue 9120 ack "
                            + "serialNo={} debug={}",
                    LogSanitizer.sanitize(safeSerial), LogSanitizer.sanitize(debugReason));
        } else {
            persistRecord(event, safeSerial);
        }
        enqueue9120Ack(event, debugReason, safeSerial);
        LOG.info("2101 processed serialNo={} body={}",
                LogSanitizer.sanitize(safeSerial),
                LogSanitizer.sanitize(body != null ? "decoded" : "null"));
    }

    private void persistRecord(final InboundMessageProcessedEvent event, final String safeSerial) {
        try {
            final RecordCreateRequest req = new RecordCreateRequest();
            req.setMessageCode("2101");
            req.setSerialNo(event.serialNo());
            req.setDirection(MessageDirection.INBOUND);
            recordService.create(req);
        } catch (final FepBusinessException dup) {
            if (dup.getErrorCode() == FepErrorCode.BIZ_5002) {
                LOG.warn("2101 listener: record dup serialNo={}, continue 9120 ack (idempotent)",
                        LogSanitizer.sanitize(safeSerial));
            } else {
                throw dup;
            }
        }
    }

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
                "2101-serialNo:" + event.serialNo());
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
     * Deterministic 32-hex idempotency key derived from serialNo so retransmissions
     * of the same 2101 collapse to the same ack envelope row via the
     * {@code uk_outbound_queue_idempotency_key} UNIQUE constraint.
     *
     * @param serialNo the inbound 2101 business serial number, non-null
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
