package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.config.CallbackAlertEmailProperties;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackEmailAlertChannel} 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackEmailAlertChannelTest {

    @Mock JavaMailSender mailSender;

    @SuppressWarnings("unchecked")
    private CallbackEmailAlertChannel channel() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        lenient().when(provider.getIfAvailable()).thenReturn(mailSender);
        return new CallbackEmailAlertChannel(provider,
                new CallbackAlertEmailProperties("fep-alert@example.com"));
    }

    @SuppressWarnings("unchecked")
    private CallbackEmailAlertChannel channelNoSender() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new CallbackEmailAlertChannel(provider,
                new CallbackAlertEmailProperties("fep-alert@example.com"));
    }

    private CallbackAlertMessage msg(final String email) {
        return new CallbackAlertMessage("CALLBACK_DLQ", "ERROR", "回调死信 - IF-1", "queueId=q1",
                "q1", "CALLBACK_DLQ_ENTRY", email, null);
    }

    @Test
    void supports_onlyEmail() {
        assertThat(channel().supports(NotifyMethod.EMAIL)).isTrue();
        assertThat(channel().supports(NotifyMethod.IN_APP)).isFalse();
    }

    @Test
    void send_shouldDispatchSimpleMailMessage() {
        channel().send(msg("ops@bank.com"));
        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(cap.capture());
        assertThat(cap.getValue().getTo()).containsExactly("ops@bank.com");
        assertThat(cap.getValue().getFrom()).isEqualTo("fep-alert@example.com");
        assertThat(cap.getValue().getSubject()).contains("IF-1");
    }

    @Test
    void send_shouldSkipWhenNoRecipient() {
        channel().send(msg(null));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_shouldSkipWhenMailSenderUnavailable() {
        channelNoSender().send(msg("ops@bank.com"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_shouldIsolateMailException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatCode(() -> channel().send(msg("ops@bank.com"))).doesNotThrowAnyException();
    }
}
