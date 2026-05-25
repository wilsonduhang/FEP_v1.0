package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.dto.RecordResponse;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Parameterized unit tests for the 4 P4-MSG-K inbound 9120-ack listeners
 * (3105/3009/3103/3113). Base-class behavior (8 ACs) is covered by
 * {@code BizMessage2101InboundListenerTest} + {@code BizMessage3112InboundListenerTest};
 * this verifies each subclass's wiring (type guard, messageCode, bodyClass, namespaced key)
 * + the persist+ack acceptance closure (PRD §4.6 受理侧, muzhou Q1 全返 9120).
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("P4-MSG-K 4 inbound listener — 持久化 + 9120 ack 受理闭环")
class BizMessageAck9120InboundListenerTest {

    private static final String INSTITUTION_CODE = "12345678901234";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-25T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private BizMessageRecordService recordService;
    @Mock
    private OutboundMessageEnqueuePort enqueuePort;

    /** factory: (recordService,enqueuePort)->listener ; body factory ; msgNo ; MessageType. */
    static Stream<Arguments> listeners() {
        return Stream.of(
                Arguments.of("3105", MessageType.MSG_3105,
                        (java.util.function.Supplier<Object>) RzApplyInfo3105::new),
                Arguments.of("3009", MessageType.MSG_3009,
                        (java.util.function.Supplier<Object>) RzReturnInfo3009::new),
                Arguments.of("3103", MessageType.MSG_3103,
                        (java.util.function.Supplier<Object>) ArchiveReturnInfo3103::new),
                Arguments.of("3113", MessageType.MSG_3113,
                        (java.util.function.Supplier<Object>) HxqyCreditAmt3113::new));
    }

    private AbstractAck9120InboundListener listenerFor(String code) {
        return switch (code) {
            case "3105" -> new BizMessage3105InboundListener(recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
            case "3009" -> new BizMessage3009InboundListener(recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
            case "3103" -> new BizMessage3103InboundListener(recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
            case "3113" -> new BizMessage3113InboundListener(recordService, enqueuePort, INSTITUTION_CODE, FIXED_CLOCK);
            default -> throw new IllegalArgumentException(code);
        };
    }

    @ParameterizedTest(name = "msg={0} 受理 → 持久化 INBOUND/{0} + 9120 ack(ACK-9120-{0}-)")
    @MethodSource("listeners")
    void onEvent_persistsAndEnqueues9120(String code, MessageType type,
                                         java.util.function.Supplier<Object> bodyFactory) {
        AbstractAck9120InboundListener listener = listenerFor(code);
        String serialNo = "SN" + code + "0001";
        InboundMessageProcessedEvent event =
                new InboundMessageProcessedEvent(type, "20260525", serialNo, bodyFactory.get(), Instant.now());
        when(recordService.create(any())).thenReturn(new RecordResponse());
        when(enqueuePort.submit(any())).thenReturn(new EnqueueResult("Q", EnqueueResult.Status.ENQUEUED));

        listener.onProcessed(event);

        ArgumentCaptor<RecordCreateRequest> rec = ArgumentCaptor.forClass(RecordCreateRequest.class);
        verify(recordService).create(rec.capture());
        assertThat(rec.getValue().getMessageCode()).isEqualTo(code);
        assertThat(rec.getValue().getSerialNo()).isEqualTo(serialNo);
        assertThat(rec.getValue().getDirection()).isEqualTo(MessageDirection.INBOUND);

        ArgumentCaptor<OutboundMessageEnvelope> env = ArgumentCaptor.forClass(OutboundMessageEnvelope.class);
        verify(enqueuePort).submit(env.capture());
        assertThat(env.getValue().messageType()).isEqualTo("9120");
        assertThat(env.getValue().idempotencyKey()).isEqualTo(AckIdempotencyKeys.derive(code, serialNo));
        assertThat(env.getValue().sourceRef()).isEqualTo(code + "-serialNo:" + serialNo);
        assertThat(((MsgReturn9120) env.getValue().messageBody()).getOriMsgNo()).isEqualTo(serialNo);
    }

    @ParameterizedTest(name = "msg={0} 非本类 event → 早返回 0 调用")
    @MethodSource("listeners")
    void onForeignEvent_returnsEarly(String code, MessageType type,
                                     java.util.function.Supplier<Object> bodyFactory) {
        AbstractAck9120InboundListener listener = listenerFor(code);
        // MSG_9120 is never a registered inbound business type → foreign to every listener
        InboundMessageProcessedEvent foreign =
                new InboundMessageProcessedEvent(MessageType.MSG_9120, "20260525", "SNx", new Object(), Instant.now());

        listener.onProcessed(foreign);

        verify(recordService, never()).create(any());
        verify(enqueuePort, never()).submit(any());
    }
}
