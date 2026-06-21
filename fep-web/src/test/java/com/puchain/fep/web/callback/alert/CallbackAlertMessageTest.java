package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackAlertMessage} 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackAlertMessageTest {

    @Test
    void ofDeadLetter_shouldComposeTitleAndBody() {
        CallbackDeadLetterEvent ev = new CallbackDeadLetterEvent(
                "q1", "IF-001", "9120", 3, "HTTP 500", LocalDateTime.now());
        CallbackAlertMessage msg = CallbackAlertMessage.ofDeadLetter(ev, "a@b.com", "13800000000");
        assertThat(msg.category()).isEqualTo("CALLBACK_DLQ");
        assertThat(msg.title()).contains("IF-001");
        assertThat(msg.body()).contains("q1").contains("9120").contains("3");
        assertThat(msg.level()).isEqualTo("ERROR");
        assertThat(msg.refId()).isEqualTo("q1");
        assertThat(msg.refType()).isEqualTo("CALLBACK_DLQ_ENTRY");
        assertThat(msg.alertEmail()).isEqualTo("a@b.com");
        assertThat(msg.alertPhone()).isEqualTo("13800000000");
    }

    @Test
    void ofTlqOutboundDeadLetter_shouldComposeWithTlqCategory() {
        TlqOutboundDeadLetterEvent ev = new TlqOutboundDeadLetterEvent(
                "q9", null, 5, "TLQ send fail", LocalDateTime.now());
        CallbackAlertMessage msg = CallbackAlertMessage.ofTlqOutboundDeadLetter(ev, "a@b.com", "13800000000");
        assertThat(msg.category()).isEqualTo("TLQ_OUTBOUND_DLQ");
        assertThat(msg.refType()).isEqualTo("TLQ_OUTBOUND_DLQ_ENTRY");
        assertThat(msg.title()).contains("q9");
        assertThat(msg.body()).contains("q9").contains("5").contains("TLQ send fail");
        assertThat(msg.level()).isEqualTo("ERROR");
        assertThat(msg.refId()).isEqualTo("q9");
        assertThat(msg.alertEmail()).isEqualTo("a@b.com");
        assertThat(msg.alertPhone()).isEqualTo("13800000000");
    }
}
