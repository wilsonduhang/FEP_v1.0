package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.xml.XmlCodec;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TlqInboundListener}.
 *
 * <p>Covers 3 paths (P3 Task 3 v1a verification):</p>
 * <ol>
 *   <li>valid 3116 payload → dispatcher invoked once with derived msgNo + transitionNo.</li>
 *   <li>malformed XML → dispatcher untouched, error logged silently (broker ack).</li>
 *   <li>dispatcher raises FepBusinessException (unknown msgNo) → swallowed, no rethrow.</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TlqInboundListenerTest {

    /**
     * msgId 20-digit ending in transitionNo {@code 00000001} (last 8 chars).
     */
    private static final String VALID_3116_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
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

    /**
     * BatchHead3115 TransitionNo=88888888 故意 ≠ MsgId 末 8 位 00000111（反占位证伪）。
     */
    private static final String INDEPENDENT_3115_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                    + "<DesNode>B43010104B0001</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3115</MsgNo>"
                    + "<MsgId>20260424105000000111</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260424</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BatchHead3115>"
                    + "<SendOrgCode>A1000143000104</SendOrgCode>"
                    + "<EntrustDate>20260424</EntrustDate>"
                    + "<TransitionNo>88888888</TransitionNo>"
                    + "<Result>00000</Result>"
                    + "</BatchHead3115>"
                    + "<PlatPay3115>"
                    + "<SerialNo>SN2026042410500000000000000111</SerialNo>"
                    + "</PlatPay3115>"
                    + "</MSG>"
                    + "</CFX>";

    private InboundMessageDispatcher dispatcher;
    private TlqInboundListener listener;

    @BeforeEach
    void setUp() {
        dispatcher = mock(InboundMessageDispatcher.class);
        listener = new TlqInboundListener(dispatcher, new XmlCodec());
    }

    @Test
    @DisplayName("valid 3116 payload → dispatcher.dispatch invoked exactly once")
    void onMessage_valid3116_dispatchesOnce() {
        final TlqMessage message = newMessage(VALID_3116_XML);
        when(dispatcher.dispatch(eq("3116"), eq("00000001"), any(byte[].class)))
                .thenReturn(new InboundMessageResponse("rec-001", "COMPLETED", true));

        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3116"), eq("00000001"), any(byte[].class));
    }

    @Test
    @DisplayName("malformed XML → dispatcher untouched, exception swallowed")
    void onMessage_malformedXml_dispatcherUntouched() {
        final TlqMessage message = newMessage("<not-cfx>broken</not-cfx>");

        listener.onMessage(message);

        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("dispatcher throws FepBusinessException → swallowed, no rethrow")
    void onMessage_dispatcherThrows_swallowed() {
        final TlqMessage message = newMessage(VALID_3116_XML);
        when(dispatcher.dispatch(eq("3116"), eq("00000001"), any(byte[].class)))
                .thenThrow(new FepBusinessException(FepErrorCode.MSG_INBOUND_INVALID_TYPE,
                        "test-exception"));

        // Listener must not rethrow — broker should treat delivery as ack'd
        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3116"), eq("00000001"), any(byte[].class));
    }

    @Test
    @DisplayName("业务头 TransitionNo 覆盖 msgId 末 8 位派生 → dispatch 用业务头真值 88888888")
    void onMessage_bodyTransitionNo_overridesDerived() {
        final TlqMessage message = newMessage(INDEPENDENT_3115_XML);
        when(dispatcher.dispatch(eq("3115"), eq("88888888"), any(byte[].class)))
                .thenReturn(new InboundMessageResponse("rec-115", "COMPLETED", true));

        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3115"), eq("88888888"), any(byte[].class));
    }

    @Test
    @DisplayName("无业务头 TransitionNo → fallback msgId 末 8 位 00000001（向后兼容）")
    void onMessage_noBodyTransitionNo_fallsBackToDerived() {
        final TlqMessage message = newMessage(VALID_3116_XML);
        when(dispatcher.dispatch(eq("3116"), eq("00000001"), any(byte[].class)))
                .thenReturn(new InboundMessageResponse("rec-116", "COMPLETED", true));

        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3116"), eq("00000001"), any(byte[].class));
    }

    private static TlqMessage newMessage(final String xml) {
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("MSG-ID-TEST-0001");
        // payload stored as UTF-8 string (matches TlqMessage.getPayload contract)
        return new TlqMessage(new String(xml.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                attrs, TlqChannel.REALTIME_RECEIVE);
    }
}
