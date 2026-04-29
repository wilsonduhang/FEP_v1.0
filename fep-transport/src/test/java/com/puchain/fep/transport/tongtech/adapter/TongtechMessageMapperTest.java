package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.tongtech.tlq.base.TlqMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TongtechMessageMapper}.
 *
 * <p>Covers SDK field-access semantics confirmed by {@code javap} on
 * {@code com.tongtech.tlq.base.TlqMessage} (P1c Plan v1a):</p>
 * <ul>
 *   <li>{@code MsgId} / {@code CorrMsgId} are {@code byte[]} (UTF-8 encoded)</li>
 *   <li>{@code Persistence} is {@code char}; {@code TLQPER_Y/N} are {@code int} (need cast)</li>
 *   <li>{@code xmlstr} / {@code xmlstr1} / {@code xmlstr2} use JMS-style
 *       {@code setStringProperty} / {@code getStringProperty}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechMessageMapperTest {

    private final TongtechMessageMapper mapper = new TongtechMessageMapper();

    @Test
    @DisplayName("toSdk: small payload (< 8KB) → xmlstr only, xmlstr1/2 empty, Persistence=N")
    void toSdk_smallPayload_shouldFitInXmlstrOnly() {
        var fep = new com.puchain.fep.transport.model.TlqMessage(
                "<CFX>...</CFX>",
                TlqMessageAttributes.forRealtime("MSG-001"),
                TlqChannel.REALTIME_SEND);

        TlqMessage sdk = mapper.toSdkMessage(fep);

        // v1a: MsgId is byte[]
        assertThat(new String(sdk.MsgId, StandardCharsets.UTF_8).trim()).isEqualTo("MSG-001");
        assertThat(sdk.getStringProperty("xmlstr")).isEqualTo("<CFX>...</CFX>");
        assertThat(sdk.getStringProperty("xmlstr1")).isNullOrEmpty();
        assertThat(sdk.getStringProperty("xmlstr2")).isNullOrEmpty();
        // v1a: Persistence is char, TLQPER_N is int (forRealtime defaults to non-persistence)
        assertThat(sdk.Persistence).isEqualTo((char) TlqMessage.TLQPER_N);
    }

    @Test
    @DisplayName("toSdk: ~20KB payload → split across xmlstr/xmlstr1/xmlstr2, Persistence=Y")
    void toSdk_largePayload_shouldSplitIntoThreeXmlAttrs() {
        String payload = "X".repeat(20_000); // ~20KB → 3 segments
        var fep = new com.puchain.fep.transport.model.TlqMessage(
                payload, TlqMessageAttributes.forBatch("MSG-002"), TlqChannel.BATCH_SEND);

        TlqMessage sdk = mapper.toSdkMessage(fep);

        String x = sdk.getStringProperty("xmlstr");
        String x1 = sdk.getStringProperty("xmlstr1");
        String x2 = sdk.getStringProperty("xmlstr2");
        assertThat(x.length() + (x1 == null ? 0 : x1.length()) + (x2 == null ? 0 : x2.length()))
                .isEqualTo(payload.length());
        // v1a: forBatch enables persistence, Persistence is char, TLQPER_Y is int
        assertThat(sdk.Persistence).isEqualTo((char) TlqMessage.TLQPER_Y);
    }

    @Test
    @DisplayName("roundtrip: toSdk → fromSdk preserves payload + msgId + corrMsgId + flags")
    void roundtrip_shouldPreserveAll() {
        var attrs = TlqMessageAttributes.forBatch("MSG-RT");
        attrs.setCorrMsgId("CORR-001");
        attrs.setZip(true);
        attrs.setEncrypt(false);
        var original = new com.puchain.fep.transport.model.TlqMessage(
                "<CFX>roundtrip</CFX>", attrs, TlqChannel.BATCH_RECEIVE);

        TlqMessage sdk = mapper.toSdkMessage(original);
        var restored = mapper.fromSdkMessage(sdk, TlqChannel.BATCH_RECEIVE);

        assertThat(restored.getPayload()).isEqualTo(original.getPayload());
        assertThat(restored.getMsgId()).isEqualTo("MSG-RT");
        assertThat(restored.getAttributes().getCorrMsgId()).isEqualTo("CORR-001");
        assertThat(restored.getAttributes().isPersistence()).isTrue();
        assertThat(restored.getAttributes().isZip()).isTrue();
        assertThat(restored.getAttributes().isEncrypt()).isFalse();
    }

    @Test
    @DisplayName("toSdk: payload > 24KB → FepBusinessException(TRANS_7001)")
    void payloadOver24KB_shouldThrowTrans7001() {
        String oversized = "X".repeat(25_000);
        var fep = new com.puchain.fep.transport.model.TlqMessage(
                oversized, TlqMessageAttributes.forBatch("MSG-OVER"), TlqChannel.BATCH_SEND);
        // PayloadSplitter rethrows FepBusinessException with TRANS_7001 error code; the
        // exception message itself is the human-readable "Payload size N bytes exceeds..."
        // (does not embed the code string), so we assert the typed error code instead of
        // a substring match — captures the business semantics with no string coupling.
        assertThatThrownBy(() -> mapper.toSdkMessage(fep))
                .isInstanceOf(FepBusinessException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(FepBusinessException.class))
                .extracting(FepBusinessException::getErrorCode)
                .isEqualTo(FepErrorCode.TRANS_7001);
    }

    @Test
    @DisplayName("toSdk: null FEP message → NullPointerException (defensive guard)")
    void nullInput_shouldThrowNpe() {
        assertThatThrownBy(() -> mapper.toSdkMessage(null))
                .isInstanceOf(NullPointerException.class);
    }
}
