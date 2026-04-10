package com.puchain.fep.converter.transport;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.pipeline.DecodeResult;
import com.puchain.fep.converter.pipeline.EncodeResult;
import com.puchain.fep.converter.pipeline.MessageDecoder;
import com.puchain.fep.converter.pipeline.MessagePipelineOptions;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试：TransportPayloadAdapter — 编码结果 ↔ TlqMessage 适配。
 */
class TransportPayloadAdapterTest {

    private final MessageDecoder decoder = mock(MessageDecoder.class);
    private final TransportPayloadAdapter adapter = new TransportPayloadAdapter(decoder);

    @Test
    void toTlqMessage_realtime_shouldBeNonPersistent() {
        EncodeResult r = new EncodeResult("PAYLOAD", true, false);
        TlqMessage msg = adapter.toTlqMessage(r, TlqChannel.REALTIME_SEND, "20260410120000000001");

        assertThat(msg.getPayload()).isEqualTo("PAYLOAD");
        assertThat(msg.getAttributes().isZip()).isTrue();
        assertThat(msg.getAttributes().isEncrypt()).isFalse();
        assertThat(msg.getAttributes().isPersistence()).isFalse();
        assertThat(msg.getAttributes().getMsgId()).isEqualTo("20260410120000000001");
    }

    @Test
    void toTlqMessage_batch_shouldBePersistentNoExpiry() {
        EncodeResult r = new EncodeResult("PAYLOAD", false, true);
        TlqMessage msg = adapter.toTlqMessage(r, TlqChannel.BATCH_SEND, "20260410120000000001");

        assertThat(msg.getAttributes().isPersistence()).isTrue();
        assertThat(msg.getAttributes().getExpiry()).isEqualTo(-1);
        assertThat(msg.getAttributes().isZip()).isFalse();
        assertThat(msg.getAttributes().isEncrypt()).isTrue();
    }

    @Test
    void toTlqMessage_realtimeReceive_shouldAlsoBeRealtime() {
        EncodeResult r = new EncodeResult("P", false, false);
        TlqMessage msg = adapter.toTlqMessage(r, TlqChannel.REALTIME_RECEIVE, "20260410120000000001");
        assertThat(msg.getAttributes().isPersistence()).isFalse();
    }

    @Test
    void toTlqMessage_payloadOver24KB_shouldRaiseConv8007() {
        String big = "x".repeat(24 * 1024 + 1);
        EncodeResult r = new EncodeResult(big, false, false);
        assertThatThrownBy(() -> adapter.toTlqMessage(r, TlqChannel.REALTIME_SEND, "20260410120000000001"))
                .isInstanceOfSatisfying(MessageConverterException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8007);
                    assertThat(ex.getMessage()).contains("exceeds 24KB");
                    assertThat(ex.getMessage()).contains("file channel");
                });
    }

    @Test
    void toTlqMessage_payloadExactly24KB_shouldBeAccepted() {
        String borderline = "x".repeat(24 * 1024);
        EncodeResult r = new EncodeResult(borderline, false, false);
        TlqMessage msg = adapter.toTlqMessage(r, TlqChannel.REALTIME_SEND, "20260410120000000001");
        assertThat(msg.getPayload()).hasSize(24 * 1024);
    }

    @Test
    void fromTlqMessage_shouldOverrideOptionsFromAttributesWithoutMutatingCaller() {
        TlqMessageAttributes attrs = TlqMessageAttributes.forBatch("20260410120000000001");
        attrs.setZip(true);
        attrs.setEncrypt(true);
        TlqMessage tlq = new TlqMessage("PAYLOAD", attrs, TlqChannel.BATCH_SEND);

        MessagePipelineOptions callerOpts = new MessagePipelineOptions();
        callerOpts.setZip(false);
        callerOpts.setEncrypt(false);

        ArgumentCaptor<MessagePipelineOptions> captor =
                ArgumentCaptor.forClass(MessagePipelineOptions.class);
        DecodeResult fakeResult = mock(DecodeResult.class);
        when(decoder.decode(eq("PAYLOAD"), captor.capture())).thenReturn(fakeResult);

        DecodeResult r = adapter.fromTlqMessage(tlq, callerOpts);

        // 调用方 opts 未被修改（无副作用）
        assertThat(callerOpts.isZip()).isFalse();
        assertThat(callerOpts.isEncrypt()).isFalse();

        // decoder 收到的是不同实例（副本），且 zip/encrypt 被覆盖为 attrs 的值
        MessagePipelineOptions passed = captor.getValue();
        assertThat(passed).isNotSameAs(callerOpts);
        assertThat(passed.isZip()).isTrue();
        assertThat(passed.isEncrypt()).isTrue();
        assertThat(r).isSameAs(fakeResult);
    }
}
