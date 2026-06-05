package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.sms.CallbackSmsGateway;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link CallbackSmsAlertChannel} 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackSmsAlertChannelTest {

    @Mock CallbackSmsGateway gateway;

    private CallbackAlertMessage msg(final String phone) {
        return new CallbackAlertMessage("ERROR", "回调死信 - IF-1", "queueId=q1",
                "q1", "CALLBACK_DLQ_ENTRY", null, phone);
    }

    @Test
    void supports_onlySms() {
        CallbackSmsAlertChannel ch = new CallbackSmsAlertChannel(gateway);
        assertThat(ch.supports(NotifyMethod.SMS)).isTrue();
        assertThat(ch.supports(NotifyMethod.EMAIL)).isFalse();
    }

    @Test
    void send_shouldDelegateToGateway() {
        new CallbackSmsAlertChannel(gateway).send(msg("13800000000"));
        verify(gateway).send(eq("13800000000"), any());
    }

    @Test
    void send_shouldSkipWhenNoPhone() {
        new CallbackSmsAlertChannel(gateway).send(msg(null));
        verify(gateway, never()).send(any(), any());
    }
}
