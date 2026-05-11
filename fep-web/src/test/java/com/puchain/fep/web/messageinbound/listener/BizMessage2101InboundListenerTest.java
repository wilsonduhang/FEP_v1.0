package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.batch.DataTransfer2101;
import com.puchain.fep.processor.body.common.MsgReturn9120;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BizMessage2101InboundListener} — FR-MSG-2101 mode 6
 * 持久化 + 9120 ack (PRD §1.4 mode 6 line 863 + §4.3 line 770).
 *
 * <p>Covers AC-1..AC-10 (8 cases: positive + 7 critical/edge paths).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BizMessage2101InboundListener — FR-MSG-2101 mode 6 持久化 + 9120 ack")
class BizMessage2101InboundListenerTest {

    private static final String INSTITUTION_CODE = "12345678901234"; // 14-digit
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-11T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private BizMessageRecordService recordService;
    @Mock
    private OutboundMessageEnqueuePort enqueuePort;

    private BizMessage2101InboundListener listener;

    @BeforeEach
    void setUp() {
        listener = new BizMessage2101InboundListener(
                recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
    }

    private static InboundMessageProcessedEvent event2101(final String serialNo, final Object body) {
        return new InboundMessageProcessedEvent(
                MessageType.MSG_2101, "20260511", serialNo, body, Instant.now());
    }

    @Test
    @DisplayName("AC-1+2+3: 2101 event → 持久化 + 9120 ack 入队（envelope 完整 7 字段）")
    void on2101Event_shouldPersistRecordAndEnqueue9120Ack() {
        DataTransfer2101 body = new DataTransfer2101();
        InboundMessageProcessedEvent event = event2101("SN20260511000001", body);
        when(recordService.create(any())).thenReturn(new RecordResponse());
        when(enqueuePort.submit(any()))
                .thenReturn(new EnqueueResult("Q-001", EnqueueResult.Status.ENQUEUED));

        listener.onProcessed(event);

        ArgumentCaptor<RecordCreateRequest> recordCap = ArgumentCaptor.forClass(RecordCreateRequest.class);
        verify(recordService).create(recordCap.capture());
        assertThat(recordCap.getValue().getMessageCode()).isEqualTo("2101");
        assertThat(recordCap.getValue().getSerialNo()).isEqualTo("SN20260511000001");
        assertThat(recordCap.getValue().getDirection()).isEqualTo(MessageDirection.INBOUND);

        ArgumentCaptor<OutboundMessageEnvelope> envCap =
                ArgumentCaptor.forClass(OutboundMessageEnvelope.class);
        verify(enqueuePort).submit(envCap.capture());
        OutboundMessageEnvelope env = envCap.getValue();
        assertThat(env.messageType()).isEqualTo("9120");
        assertThat(env.direction()).isEqualTo(Direction.OUTBOUND);
        assertThat(env.idempotencyKey()).hasSize(32);
        assertThat(env.headFields().sendOrgCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(env.headFields().entrustDate()).matches("\\d{8}");
        assertThat(env.headFields().transitionNo()).isEqualTo("20260511");
        assertThat(env.messageBody()).isInstanceOf(MsgReturn9120.class);
        MsgReturn9120 ack = (MsgReturn9120) env.messageBody();
        assertThat(ack.getOriMsgNo()).isEqualTo("SN20260511000001");
        assertThat(ack.getDebug()).isNull();
        assertThat(env.payloadDataType()).isEqualTo("ACK_9120");
        assertThat(env.sourceRef()).isEqualTo("2101-serialNo:SN20260511000001");
    }

    @Test
    @DisplayName("AC-4: 非 2101 event → 早返回，0 mock 调用")
    void onNon2101Event_shouldReturnEarly() {
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260511", "SN2", new Object(), Instant.now());

        listener.onProcessed(event);

        verify(recordService, never()).create(any());
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-5: body=null → 跳过 record + 发 9120 ack with debug=BODY_NULL_OR_UNMARSHAL_SKIPPED")
    void on2101EventWithNullBody_shouldStillEnqueue9120WithDebug() {
        InboundMessageProcessedEvent event = event2101("SN3", null);
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
    void on2101EventWithDupSerial_shouldStillEnqueue9120() {
        DataTransfer2101 body = new DataTransfer2101();
        InboundMessageProcessedEvent event = event2101("SN4", body);
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
    void on2101EventWithNonDupRecordException_shouldRethrowToTriggerRollback() {
        DataTransfer2101 body = new DataTransfer2101();
        InboundMessageProcessedEvent event = event2101("SN5", body);
        when(recordService.create(any())).thenThrow(
                new FepBusinessException(FepErrorCode.BIZ_5003, "业务状态不允许"));

        assertThatThrownBy(() -> listener.onProcessed(event))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.BIZ_5003);
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-8: COLLECT_DUPLICATE_KEY → WARN + 静默忽略（at-least-once 兜底）")
    void on2101EventWithDupAckKey_shouldSwallowDupKey() {
        DataTransfer2101 body = new DataTransfer2101();
        InboundMessageProcessedEvent event = event2101("SN6", body);
        when(recordService.create(any())).thenReturn(new RecordResponse());
        doThrow(new FepBusinessException(FepErrorCode.COLLECT_DUPLICATE_KEY, "dup"))
                .when(enqueuePort).submit(any());

        listener.onProcessed(event);

        verify(enqueuePort).submit(any());
    }

    @Test
    @DisplayName("AC-9: bodyAs type mismatch → IllegalStateException 透传触发 rollback")
    void on2101EventWithBodyTypeMismatch_shouldPropagateISE() {
        InboundMessageProcessedEvent event = event2101("SN7", new Object());

        assertThatThrownBy(() -> listener.onProcessed(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event.body type mismatch");
        verify(recordService, never()).create(any());
        verify(enqueuePort, never()).submit(any());
    }

    @Test
    @DisplayName("AC-10: institutionCode 空 → FepBusinessException(COLLECT_ASSEMBLE_FAILURE)")
    void on2101EventWithBlankInstitutionCode_shouldThrow() {
        BizMessage2101InboundListener brokenListener =
                new BizMessage2101InboundListener(recordService, enqueuePort, "", FIXED_CLOCK);
        DataTransfer2101 body = new DataTransfer2101();
        InboundMessageProcessedEvent event = event2101("SN8", body);
        when(recordService.create(any())).thenReturn(new RecordResponse());

        assertThatThrownBy(() -> brokenListener.onProcessed(event))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
        verify(enqueuePort, never()).submit(any());
    }
}
