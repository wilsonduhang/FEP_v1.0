package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.transport.api.MessageListener;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.support.QueueNameResolver.QueueType;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqMsgOpt;
import com.tongtech.tlq.base.TlqQCU;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TongtechTlqConsumer}.
 *
 * <p>Validates Plan v1d Task 6 acceptance criteria (lines 1654-1855):</p>
 * <ul>
 *   <li>Pull-mode {@link TongtechTlqConsumer#receive} maps SDK rc==0 → present
 *       Optional + immediate ackMessage(TLQACK_COMMIT); rc!=0 → empty Optional.</li>
 *   <li>{@link TlqException} during {@code getMessage} maps to
 *       {@link FepBusinessException} via {@link TongtechErrorMapper}.</li>
 *   <li>Push-mode {@link TongtechTlqConsumer#subscribe} is idempotent per channel
 *       and dispatches via {@link TongtechTlqConsumer#pollAndDispatch} —
 *       success acks, listener throw skips ack (re-delivery), duplicate msgId
 *       acks but does not invoke listener.</li>
 *   <li>{@link TongtechTlqConsumer#unsubscribe} cancels the active
 *       {@link ScheduledFuture}.</li>
 * </ul>
 *
 * <p>Note: subscribe tests exercise the package-private
 * {@link TongtechTlqConsumer#pollAndDispatch} directly so that the
 * {@link java.util.concurrent.ScheduledExecutorService} polling cadence does not
 * introduce timing flakiness. The scheduler scheduling itself is verified by
 * the idempotency and unsubscribe cases.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechTlqConsumerTest {

    private TongtechTlqConnectionFactory factory;
    private TongtechMessageMapper mapper;
    private QueueNameResolver resolver;
    private TongtechChannelMapper channelMapper;
    private MessageDeduplicator deduplicator;
    private TongtechErrorMapper errorMapper;
    private TongtechTlqProperties props;
    private TlqQCU qcu;
    private TongtechTlqConsumer consumer;

    @BeforeEach
    void setUp() {
        factory = mock(TongtechTlqConnectionFactory.class);
        mapper = mock(TongtechMessageMapper.class);
        resolver = mock(QueueNameResolver.class);
        channelMapper = mock(TongtechChannelMapper.class);
        deduplicator = mock(MessageDeduplicator.class);
        errorMapper = mock(TongtechErrorMapper.class);
        props = mock(TongtechTlqProperties.class);
        qcu = mock(TlqQCU.class);

        when(factory.isConnected()).thenReturn(true);
        when(factory.getQCU()).thenReturn(qcu);
        when(channelMapper.toQueueType(TlqChannel.REALTIME_RECEIVE))
                .thenReturn(QueueType.REALTIME_DEST);
        when(resolver.resolve(QueueType.REALTIME_DEST))
                .thenReturn("QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        when(props.getConsumerPollIntervalMs()).thenReturn(100L);

        consumer = new TongtechTlqConsumer(
                factory, mapper, resolver, channelMapper,
                deduplicator, errorMapper, props);
    }

    /** Configure {@code qcu.getMessage(...)} to return rc==0 and stub the mapper to a FEP message. */
    private void stubGetMessageReturnsMsg(final String msgId) throws TlqException {
        doAnswer(inv -> 0).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime(msgId);
        final TlqMessage fep = new TlqMessage("<CFX/>", attrs, TlqChannel.REALTIME_RECEIVE);
        when(mapper.fromSdkMessage(any(com.tongtech.tlq.base.TlqMessage.class), eq(TlqChannel.REALTIME_RECEIVE)))
                .thenReturn(fep);
    }

    @Test
    @DisplayName("receive: rc != 0 (no message in window) returns Optional.empty and does not ack")
    void receive_timeout_shouldReturnEmpty() throws TlqException {
        // rc == 1 means no message available within wait window
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));

        final Optional<TlqMessage> result = consumer.receive(
                TlqChannel.REALTIME_RECEIVE, Duration.ofMillis(50));

        assertThat(result).isEmpty();
        verify(qcu, never()).ackMessage(any(), any(TlqMsgOpt.class), anyInt());
    }

    @Test
    @DisplayName("receive: success path immediately acks with TLQACK_COMMIT and returns mapped FEP message")
    void receive_success_shouldAckCommitImmediately() throws TlqException {
        stubGetMessageReturnsMsg("M-RX-1");

        final Optional<TlqMessage> result = consumer.receive(
                TlqChannel.REALTIME_RECEIVE, Duration.ofMillis(100));

        assertThat(result).isPresent();
        assertThat(result.get().getMsgId()).isEqualTo("M-RX-1");
        verify(qcu).ackMessage(
                any(com.tongtech.tlq.base.TlqMessage.class),
                any(TlqMsgOpt.class),
                eq(TlqMsgOpt.TLQACK_COMMIT));

        // Validate the QName + AckMode + WaitInterval that we set on the SDK opt
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).getMessage(any(), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo("QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        assertThat((int) optCap.getValue().AckMode).isEqualTo(TlqMsgOpt.TLQACK_USER);
        assertThat(optCap.getValue().WaitInterval).isEqualTo(100);
    }

    @Test
    @DisplayName("receive: TlqException from getMessage maps to FepBusinessException via errorMapper")
    void receive_sdkException_shouldMapToTrans7005() throws TlqException {
        final TlqException tle = mock(TlqException.class);
        when(tle.getErrorCause()).thenReturn("receive timeout polling failed");
        doThrow(tle).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final FepBusinessException mapped = new FepBusinessException(
                FepErrorCode.TRANS_7005, "receive failed: receive timeout polling failed", tle);
        when(errorMapper.mapException(tle)).thenReturn(mapped);

        assertThatThrownBy(() -> consumer.receive(
                TlqChannel.REALTIME_RECEIVE, Duration.ofMillis(100)))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.TRANS_7005);

        verify(errorMapper).mapException(tle);
        verify(qcu, never()).ackMessage(any(), any(TlqMsgOpt.class), anyInt());
    }

    @Test
    @DisplayName("receive: lazy connect when factory not yet connected")
    void receive_lazyConnect_whenNotConnected() throws TlqException {
        when(factory.isConnected()).thenReturn(false);
        stubGetMessageReturnsMsg("M-RX-LC");

        consumer.receive(TlqChannel.REALTIME_RECEIVE, Duration.ofMillis(50));

        verify(factory).connect();
    }

    @Test
    @DisplayName("subscribe: invoking twice on the same channel is idempotent (single scheduler task)")
    void subscribe_isIdempotent() throws TlqException {
        // Stub getMessage rc=1 (timeout) so the daemon poller stays quiet while
        // we assert idempotency — without this the polling thread NPEs against
        // the un-stubbed mapper.fromSdkMessage and floods the test log.
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final MessageListener listener = mock(MessageListener.class);

        consumer.subscribe(TlqChannel.REALTIME_RECEIVE, listener);
        // Capture the future created by the first subscribe
        final ScheduledFuture<?> first = consumer.subscriptionsForTest()
                .get(TlqChannel.REALTIME_RECEIVE);

        consumer.subscribe(TlqChannel.REALTIME_RECEIVE, listener);
        final ScheduledFuture<?> second = consumer.subscriptionsForTest()
                .get(TlqChannel.REALTIME_RECEIVE);

        assertThat((Object) first).isNotNull();
        assertThat((Object) second).isSameAs(first); // same task, no replacement

        // Cleanup so the daemon poller does not race other tests
        consumer.shutdown();
    }

    @Test
    @DisplayName("pollAndDispatch: listener success → ackMessage(TLQACK_COMMIT)")
    void subscribe_dispatch_shouldAckOnListenerSuccess() throws TlqException {
        stubGetMessageReturnsMsg("M-PUSH-OK");
        when(deduplicator.isDuplicate("M-PUSH-OK")).thenReturn(false);
        final MessageListener listener = mock(MessageListener.class);

        consumer.pollAndDispatch(TlqChannel.REALTIME_RECEIVE, listener);

        verify(listener).onMessage(any(TlqMessage.class));
        verify(qcu).ackMessage(
                any(com.tongtech.tlq.base.TlqMessage.class),
                any(TlqMsgOpt.class),
                eq(TlqMsgOpt.TLQACK_COMMIT));
    }

    @Test
    @DisplayName("pollAndDispatch: listener throws → no ack (broker re-delivers / DLQ)")
    void subscribe_listenerThrows_shouldNotAck() throws TlqException {
        stubGetMessageReturnsMsg("M-PUSH-THROW");
        when(deduplicator.isDuplicate("M-PUSH-THROW")).thenReturn(false);
        final MessageListener listener = mock(MessageListener.class);
        doThrow(new IllegalStateException("downstream busy")).when(listener)
                .onMessage(any(TlqMessage.class));

        consumer.pollAndDispatch(TlqChannel.REALTIME_RECEIVE, listener);

        verify(listener).onMessage(any(TlqMessage.class));
        verify(qcu, never()).ackMessage(any(), any(TlqMsgOpt.class), anyInt());
    }

    @Test
    @DisplayName("pollAndDispatch: duplicate msgId → ack to drop, no listener invocation")
    void subscribe_duplicateMsg_shouldAckButNotDispatch() throws TlqException {
        stubGetMessageReturnsMsg("M-PUSH-DUP");
        when(deduplicator.isDuplicate("M-PUSH-DUP")).thenReturn(true);
        final MessageListener listener = mock(MessageListener.class);

        consumer.pollAndDispatch(TlqChannel.REALTIME_RECEIVE, listener);

        verifyNoInteractions(listener);
        verify(qcu, times(1)).ackMessage(
                any(com.tongtech.tlq.base.TlqMessage.class),
                any(TlqMsgOpt.class),
                eq(TlqMsgOpt.TLQACK_COMMIT));
    }

    @Test
    @DisplayName("pollAndDispatch: rc != 0 (no message) → no listener and no ack")
    void subscribe_pollEmpty_shouldDoNothing() throws TlqException {
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final MessageListener listener = mock(MessageListener.class);

        consumer.pollAndDispatch(TlqChannel.REALTIME_RECEIVE, listener);

        verifyNoInteractions(listener);
        verify(qcu, never()).ackMessage(any(), any(TlqMsgOpt.class), anyInt());
    }

    @Test
    @DisplayName("unsubscribe: cancels the active ScheduledFuture and removes the registration")
    void unsubscribe_cancelsSchedule() throws TlqException {
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final MessageListener listener = mock(MessageListener.class);
        consumer.subscribe(TlqChannel.REALTIME_RECEIVE, listener);
        final ScheduledFuture<?> future = consumer.subscriptionsForTest()
                .get(TlqChannel.REALTIME_RECEIVE);
        assertThat((Object) future).isNotNull();

        consumer.unsubscribe(TlqChannel.REALTIME_RECEIVE);

        assertThat(future.isCancelled()).isTrue();
        assertThat(consumer.subscriptionsForTest()).doesNotContainKey(TlqChannel.REALTIME_RECEIVE);

        // Calling unsubscribe a second time is idempotent (no exception)
        consumer.unsubscribe(TlqChannel.REALTIME_RECEIVE);

        consumer.shutdown();
    }

    @Test
    @DisplayName("shutdown: cancels all subscriptions and stops the scheduler")
    void shutdown_cancelsAllSubscriptions_andStopsScheduler() throws TlqException {
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));
        final MessageListener listener = mock(MessageListener.class);
        consumer.subscribe(TlqChannel.REALTIME_RECEIVE, listener);
        final ScheduledFuture<?> future = consumer.subscriptionsForTest()
                .get(TlqChannel.REALTIME_RECEIVE);
        assertThat((Object) future).isNotNull();

        consumer.shutdown();

        assertThat(future.isCancelled()).isTrue();
        assertThat(consumer.subscriptionsForTest()).isEmpty();
        assertThat(consumer.schedulerForTest().isShutdown()).isTrue();
    }
}
