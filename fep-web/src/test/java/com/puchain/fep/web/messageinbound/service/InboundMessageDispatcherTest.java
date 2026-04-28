package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InboundMessageDispatcher}.
 *
 * <p>Covers 5 paths (P3 Task 2 v1a verification §7):</p>
 * <ol>
 *   <li>pipeline COMPLETED → publishEvent invoked exactly once</li>
 *   <li>pipeline FAILED → publishEvent never invoked</li>
 *   <li>unknown messageType → throw {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_INVALID_TYPE})</li>
 *   <li>unmarshal failure → throw {@link FepBusinessException}({@link FepErrorCode#MSG_INBOUND_DECODE_FAILURE})
 *       so the surrounding {@code @Transactional} rolls back</li>
 *   <li>event field values are populated from the unmarshalled body
 *       (asserted via {@link ArgumentCaptor})</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InboundMessageDispatcherTest {

    /**
     * Minimal CFX wrapper carrying a {@code BankCheckDay3116} body with only the
     * leading {@code SerialNo} populated. JAXB unmarshal accepts missing
     * non-mandatory elements as long as no unknown elements are present
     * (jaxb-runtime strict {@code ValidationEventHandler} rejects only
     * <em>unexpected</em> elements, not missing ones).
     */
    private static final String VALID_3116_XML_TEMPLATE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B2000456000204</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3116</MsgNo>"
                    + "<MsgId>20260428000000000001</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260428</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BankCheckDay3116>"
                    + "<SerialNo>SN20260428BANK</SerialNo>"
                    + "</BankCheckDay3116>"
                    + "</MSG>"
                    + "</CFX>";

    private SyncMessageProcessorService syncProcessor;
    private ApplicationEventPublisher eventPublisher;
    private InboundMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        syncProcessor = mock(SyncMessageProcessorService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        dispatcher = new InboundMessageDispatcher(syncProcessor, eventPublisher);
    }

    @Test
    @DisplayName("pipeline COMPLETED → publishEvent invoked once with correct fields")
    void dispatch_completed_publishesEventOnce() {
        final byte[] xml = VALID_3116_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-001abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xml)))
                .thenReturn(completed);

        final InboundMessageResponse response = dispatcher.dispatch("3116", "20260428", xml);

        assertThat(response.recordId()).isEqualTo("rec-001abcdef0123456789abcdef01230000");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.eventPublished()).isTrue();

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        assertThat(event.type()).isEqualTo(MessageType.MSG_3116);
        assertThat(event.transitionNo()).isEqualTo("20260428");
        assertThat(event.serialNo()).isEqualTo("SN20260428BANK");
        assertThat(event.body()).isInstanceOf(BankCheckDay3116.class);
        assertThat(event.occurredAt()).isAfter(Instant.now().minusSeconds(5));
    }

    @Test
    @DisplayName("pipeline FAILED → publishEvent is never invoked")
    void dispatch_failed_doesNotPublishEvent() {
        final byte[] xml = VALID_3116_XML_TEMPLATE.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord failed = MessageProcessRecord.initial(
                        "rec-002abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withFailure(FepErrorCode.PROC_8501.getCode(), "xsd error", Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xml)))
                .thenReturn(failed);

        final InboundMessageResponse response = dispatcher.dispatch("3116", "20260428", xml);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.eventPublished()).isFalse();
        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("unknown messageType → throw FepBusinessException(MSG_8701) and never call processor")
    void dispatch_unknownMessageType_throwsBusinessException() {
        final byte[] xml = "<CFX/>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> dispatcher.dispatch("9999", "20260428", xml))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.MSG_INBOUND_INVALID_TYPE);
                });

        verify(syncProcessor, never())
                .processInbound(any(MessageType.class), any(String.class), any(byte[].class));
        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("unmarshal failure → throw FepBusinessException(MSG_8702) so @Transactional rolls back")
    void dispatch_unmarshalFailure_throwsDecodeFailure() {
        final byte[] malformedXml = "<not-cfx>broken</not-cfx>".getBytes(StandardCharsets.UTF_8);
        // Pipeline ran fine (XSD validator on caller side did not catch — e.g. mocked away
        // in this scope), but body unmarshal must still surface as a hard rollback signal.
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-003abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(malformedXml)))
                .thenReturn(completed);

        assertThatThrownBy(() -> dispatcher.dispatch("3116", "20260428", malformedXml))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.MSG_INBOUND_DECODE_FAILURE);
                });

        verify(eventPublisher, never()).publishEvent(any(InboundMessageProcessedEvent.class));
    }

    @Test
    @DisplayName("body type registry exposes 4 P3 Phase 2 entries")
    void bodyTypeRegistry_contains4P3Phase2Entries() {
        // grep-asserted (feedback_doc_data_grep_first): registry must expose
        // exactly the 4 P3 Phase 2 messageTypes wired in this Plan.
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(4);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).containsKeys("3107", "3108", "3115", "3116");
        assertThat(InboundMessageDispatcher.bodyTypeRegistry().get("3116"))
                .isEqualTo(BankCheckDay3116.class);
    }

    /**
     * P3 Task 5 regression guard for {@code feedback_dispatcher_payload_shape_blind_spot}.
     *
     * <p>Production CFX {@code <MSG>} envelope is XSD-mandated to carry
     * BatchHeadXxxx (DOM Element via lax-mode JAXB) <em>before</em> the body
     * POJO. The pre-fix {@code getBody()} call returned position-0 BatchHead
     * Element, masking the real body POJO and silently producing
     * {@code event.body=null}. This test ensures the {@code getBodies()::isInstance}
     * filter still picks the registered body class even when BatchHead is in
     * front, regardless of any future {@code getBody()} regression.</p>
     */
    @Test
    @DisplayName("CFX MSG with BatchHead+body siblings → dispatcher picks body POJO via isInstance filter")
    void dispatch_msgWithBatchHeadBeforeBody_picksBodyPojoNotBatchHead() {
        // BatchHead3116 sibling appears BEFORE BankCheckDay3116 — XSD-required
        // sequence. JAXB lax mode keeps unknown BatchHead3116 element as DOM
        // Element while BankCheckDay3116 deserializes to the registered POJO.
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>A1000143000104</SrcNode>"
                + "<DesNode>B2000456000204</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>3116</MsgNo>"
                + "<MsgId>20260428000000000099</MsgId>"
                + "<CorrMsgId></CorrMsgId>"
                + "<WorkDate>20260428</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + "<BatchHead3116>"
                + "<TotalNum>1</TotalNum>"
                + "<TotalAmt>100.00</TotalAmt>"
                + "</BatchHead3116>"
                + "<BankCheckDay3116>"
                + "<SerialNo>SN20260428BATCH</SerialNo>"
                + "</BankCheckDay3116>"
                + "</MSG>"
                + "</CFX>";
        final byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-099abcdef0123456789abcdef01230000",
                        MessageType.MSG_3116, "20260428", Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(MessageType.MSG_3116), eq("20260428"), eq(xmlBytes)))
                .thenReturn(completed);

        dispatcher.dispatch("3116", "20260428", xmlBytes);

        final ArgumentCaptor<InboundMessageProcessedEvent> captor =
                ArgumentCaptor.forClass(InboundMessageProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        final InboundMessageProcessedEvent event = captor.getValue();
        // body MUST be the registered POJO, not the BatchHead DOM Element
        assertThat(event.body())
                .as("dispatcher must pick BankCheckDay3116 instance, not the leading BatchHead3116 sibling")
                .isInstanceOf(BankCheckDay3116.class);
        assertThat(event.serialNo()).isEqualTo("SN20260428BATCH");
    }
}
