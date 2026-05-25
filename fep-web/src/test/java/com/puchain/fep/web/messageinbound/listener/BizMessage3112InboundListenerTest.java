package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.dto.RecordResponse;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BizMessage3112InboundListener} — FR-MSG-3112 inbound
 * bank-side passive receive (PRD §4.6 line 841 + §4.7 line 862 mode 5
 * "HNDEMP→外联先返9120应答"). Phase 1: persist + 9120 ack only; 3113 reply
 * assembly deferred (roadmap Plan C).
 *
 * <p>Mirrors {@code BizMessage2101InboundListenerTest} AC-1..AC-10 with the
 * 3112-specific idempotency namespace ({@code ACK-9120-3112-}) and the fact
 * that {@code HxqyCreditAmt3112} carries a real business SerialNo.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BizMessage3112InboundListener — FR-MSG-3112 mode5 持久化 + 9120 ack")
class BizMessage3112InboundListenerTest {

    private static final String INSTITUTION_CODE = "12345678901234"; // 14-digit
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-24T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private BizMessageRecordService recordService;
    @Mock
    private OutboundMessageEnqueuePort enqueuePort;

    private BizMessage3112InboundListener listener;

    @BeforeEach
    void setUp() {
        listener = new BizMessage3112InboundListener(
                recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
    }

    private static InboundMessageProcessedEvent event3112(final String serialNo, final Object body) {
        return new InboundMessageProcessedEvent(
                MessageType.MSG_3112, "20260524", serialNo, body, Instant.now());
    }

    private static HxqyCreditAmt3112 body3112() {
        HxqyCreditAmt3112 body = new HxqyCreditAmt3112();
        body.setSerialNo("SN20260524C3112");
        return body;
    }

    @Test
    @DisplayName("AC-1+2+3: 3112 event → 持久化 + 9120 ack 入队（envelope 完整 7 字段 + 3112 命名空间幂等）")
    void on3112Event_shouldPersistRecordAndEnqueue9120Ack() throws Exception {
        InboundMessageProcessedEvent event = event3112("SN20260524C3112", body3112());
        when(recordService.create(any())).thenReturn(new RecordResponse());
        when(enqueuePort.submit(any()))
                .thenReturn(new EnqueueResult("Q-001", EnqueueResult.Status.ENQUEUED));

        listener.onProcessed(event);

        ArgumentCaptor<RecordCreateRequest> recordCap = ArgumentCaptor.forClass(RecordCreateRequest.class);
        verify(recordService).create(recordCap.capture());
        assertThat(recordCap.getValue().getMessageCode()).isEqualTo("3112");
        assertThat(recordCap.getValue().getSerialNo()).isEqualTo("SN20260524C3112");
        assertThat(recordCap.getValue().getDirection()).isEqualTo(MessageDirection.INBOUND);

        ArgumentCaptor<OutboundMessageEnvelope> envCap =
                ArgumentCaptor.forClass(OutboundMessageEnvelope.class);
        verify(enqueuePort).submit(envCap.capture());
        OutboundMessageEnvelope env = envCap.getValue();
        assertThat(env.messageType()).isEqualTo("9120");
        assertThat(env.direction()).isEqualTo(Direction.OUTBOUND);
        assertThat(env.idempotencyKey()).hasSize(32);
        assertThat(env.idempotencyKey()).isEqualTo(expectedAckKey("SN20260524C3112"));
        assertThat(env.headFields().sendOrgCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(env.headFields().entrustDate()).matches("\\d{8}");
        assertThat(env.headFields().transitionNo()).isEqualTo("20260524");
        assertThat(env.messageBody()).isInstanceOf(MsgReturn9120.class);
        MsgReturn9120 ack = (MsgReturn9120) env.messageBody();
        assertThat(ack.getOriMsgNo()).isEqualTo("SN20260524C3112");
        assertThat(ack.getDebug()).isNull();
        assertThat(env.payloadDataType()).isEqualTo("ACK_9120");
        assertThat(env.sourceRef()).isEqualTo("3112-serialNo:SN20260524C3112");
    }

    @Test
    @DisplayName("AC-4: 非 3112 event → 早返回，0 mock 调用")
    void onNon3112Event_shouldReturnEarly() {
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                MessageType.MSG_2101, "20260524", "SN2", new Object(), Instant.now());

        listener.onProcessed(event);

        verify(recordService, never()).create(any());
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-5: body=null → 跳过 record + 发 9120 ack with debug=BODY_NULL_OR_UNMARSHAL_SKIPPED")
    void on3112EventWithNullBody_shouldStillEnqueue9120WithDebug() {
        InboundMessageProcessedEvent event = event3112("SN3", null);
        when(enqueuePort.submit(any()))
                .thenReturn(new EnqueueResult("Q-002", EnqueueResult.Status.ENQUEUED));

        listener.onProcessed(event);

        verify(recordService, never()).create(any());
        ArgumentCaptor<OutboundMessageEnvelope> envCap =
                ArgumentCaptor.forClass(OutboundMessageEnvelope.class);
        verify(enqueuePort).submit(envCap.capture());
        MsgReturn9120 ack = (MsgReturn9120) envCap.getValue().messageBody();
        assertThat(ack.getDebug()).isEqualTo("BODY_NULL_OR_UNMARSHAL_SKIPPED");
    }

    @Test
    @DisplayName("AC-6: serialNo dup BIZ_5002 → 容错 + 继续 9120 ack")
    void on3112EventWithDupSerial_shouldStillEnqueue9120() {
        InboundMessageProcessedEvent event = event3112("SN4", body3112());
        when(recordService.create(any())).thenThrow(
                new FepBusinessException(FepErrorCode.BIZ_5002, "已存在"));
        when(enqueuePort.submit(any()))
                .thenReturn(new EnqueueResult("Q-003", EnqueueResult.Status.ENQUEUED));

        listener.onProcessed(event);

        verify(recordService).create(any());
        verify(enqueuePort).submit(any());
    }

    @Test
    @DisplayName("AC-7: BIZ_5002 之外的 record 业务异常 → 向上抛触发 dispatcher rollback")
    void on3112EventWithNonDupRecordException_shouldRethrowToTriggerRollback() {
        InboundMessageProcessedEvent event = event3112("SN5", body3112());
        when(recordService.create(any())).thenThrow(
                new FepBusinessException(FepErrorCode.BIZ_5003, "业务状态不允许"));

        assertThatThrownBy(() -> listener.onProcessed(event))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.BIZ_5003);
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-8: COLLECT_DUPLICATE_KEY → WARN + 静默忽略（at-least-once 兜底）")
    void on3112EventWithDupAckKey_shouldSwallowDupKey() {
        InboundMessageProcessedEvent event = event3112("SN6", body3112());
        when(recordService.create(any())).thenReturn(new RecordResponse());
        doThrow(new FepBusinessException(FepErrorCode.COLLECT_DUPLICATE_KEY, "dup"))
                .when(enqueuePort).submit(any());

        listener.onProcessed(event);

        verify(enqueuePort).submit(any());
    }

    @Test
    @DisplayName("AC-9: bodyAs type mismatch → IllegalStateException 透传触发 rollback")
    void on3112EventWithBodyTypeMismatch_shouldPropagateISE() {
        InboundMessageProcessedEvent event = event3112("SN7", new Object());

        assertThatThrownBy(() -> listener.onProcessed(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event.body type mismatch");
        verify(recordService, never()).create(any());
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-10: institutionCode 空 → FepBusinessException(COLLECT_ASSEMBLE_FAILURE)")
    void on3112EventWithBlankInstitutionCode_shouldThrow() {
        BizMessage3112InboundListener brokenListener =
                new BizMessage3112InboundListener(recordService, enqueuePort, "", FIXED_CLOCK);
        InboundMessageProcessedEvent event = event3112("SN8", body3112());
        when(recordService.create(any())).thenReturn(new RecordResponse());

        assertThatThrownBy(() -> brokenListener.onProcessed(event))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
        verify(enqueuePort, never()).submit(any());
    }

    /** Mirror of {@code BizMessage3112InboundListener.deriveAckIdempotencyKey} (prefix ACK-9120-3112-). */
    private static String expectedAckKey(final String serialNo) throws Exception {
        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        final byte[] hash = sha256.digest(
                ("ACK-9120-3112-" + serialNo).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash).substring(0, 32);
    }
}
