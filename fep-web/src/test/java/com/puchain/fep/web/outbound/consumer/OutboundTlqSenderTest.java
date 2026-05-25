package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.web.outbound.consumer.OutboundTlqSender.OutboundSendOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link OutboundTlqSender} 单元测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>成功路径：outcome.success=true，msgId 与传入字面量一致，tlqSendResult 形如 {@code ok:<broker>}</li>
 *   <li>失败路径：outcome.success=false，tlqSendResult 以 {@code fail:} 开头且整体 ≤64 char</li>
 *   <li>边界：超长 error message 被精确截断到 64 char</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OutboundTlqSenderTest {

    @Mock
    private TlqProducer producer;

    @InjectMocks
    private OutboundTlqSender sender;

    @Test
    void send_success_should_return_msgId_and_truncated_send_result() {
        final String msgId = "20260504103000000001";
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("BROKER_MSG_ID_X"));

        final OutboundSendOutcome outcome = sender.send("<CFX/>", msgId);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.msgId()).isEqualTo(msgId);
        assertThat(outcome.tlqSendResult()).isEqualTo("ok:BROKER_MSG_ID_X");
    }

    @Test
    void send_failure_should_return_failure_outcome_with_error_truncated_64() {
        final String msgId = "20260504103000000002";
        when(producer.send(any(TlqMessage.class))).thenReturn(
            SendResult.fail(msgId, "connection refused: tcp://1.2.3.4:20002 broker unreachable for 30s"));

        final OutboundSendOutcome outcome = sender.send("<CFX/>", msgId);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.msgId()).isEqualTo(msgId);
        assertThat(outcome.tlqSendResult()).startsWith("fail:");
        assertThat(outcome.tlqSendResult().length()).isLessThanOrEqualTo(64);
    }

    @Test
    void send_failure_with_very_long_error_should_be_truncated_to_exactly_64_chars() {
        final String msgId = "20260504103000000003";
        // Construct an error that yields a "fail:" + error string > 64 chars
        final String longError = "x".repeat(200);
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.fail(msgId, longError));

        final OutboundSendOutcome outcome = sender.send("<CFX/>", msgId);

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.tlqSendResult()).hasSize(64);
        assertThat(outcome.tlqSendResult()).startsWith("fail:");
        // After "fail:" (5 chars) we should see 59 'x' characters
        assertThat(outcome.tlqSendResult().substring(5)).isEqualTo("x".repeat(59));
    }
}
