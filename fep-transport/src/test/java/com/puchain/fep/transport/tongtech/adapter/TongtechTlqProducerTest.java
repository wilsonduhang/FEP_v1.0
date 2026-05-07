package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.support.QueueNameResolver.QueueType;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqMsgOpt;
import com.tongtech.tlq.base.TlqQCU;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TongtechTlqProducer}.
 *
 * <p>Validates v1b acceptance criteria (Plan §Task 5):</p>
 * <ul>
 *   <li>#1-4 — putMessage + tx begin/commit/rollback orchestration</li>
 *   <li>#5 (B-P0-1) — TlqException maps to {@link SendResult#fail} (no throw),
 *       so {@code RetryableProducer} can drive retry + DLH routing</li>
 *   <li>#6 — connect() failure throws {@link FepBusinessException} (TRANS_7002)
 *       since connection-level errors are not retried at the producer layer</li>
 * </ul>
 *
 * <p>Retry-to-success and exhaust-to-DLH behaviour is already covered by
 * {@link com.puchain.fep.transport.api.RetryableProducerTest} and is intentionally
 * not duplicated here (see Task 5 commit message rationale).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechTlqProducerTest {

    private TongtechTlqConnectionFactory factory;
    private TongtechMessageMapper mapper;
    private QueueNameResolver resolver;
    private TongtechChannelMapper channelMapper;
    private TongtechErrorMapper errorMapper;
    private TlqQCU qcu;
    private TongtechTlqProducer producer;

    @BeforeEach
    void setUp() {
        factory = mock(TongtechTlqConnectionFactory.class);
        mapper = mock(TongtechMessageMapper.class);
        resolver = mock(QueueNameResolver.class);
        channelMapper = mock(TongtechChannelMapper.class);
        errorMapper = mock(TongtechErrorMapper.class);
        qcu = mock(TlqQCU.class);
        when(factory.isConnected()).thenReturn(true);
        when(factory.getQCU()).thenReturn(qcu);
        producer = new TongtechTlqProducer(factory, mapper, resolver, channelMapper, errorMapper);
    }

    private TlqMessage realtimeSendMsg(final String msgId) {
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime(msgId);
        return new TlqMessage("<CFX/>", attrs, TlqChannel.REALTIME_SEND);
    }

    @Test
    @DisplayName("send: success path returns ok and sets QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1 queue")
    void send_success_shouldReturnOk_andQueueIsRealtimeSend() throws TlqException {
        final TlqMessage msg = realtimeSendMsg("M1");
        when(channelMapper.toQueueType(TlqChannel.REALTIME_SEND)).thenReturn(QueueType.REALTIME_SEND);
        when(resolver.resolve(QueueType.REALTIME_SEND)).thenReturn("QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        when(mapper.toSdkMessage(msg)).thenReturn(mock(com.tongtech.tlq.base.TlqMessage.class));

        final SendResult result = producer.send(msg);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo("M1");
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).putMessage(any(com.tongtech.tlq.base.TlqMessage.class), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo("QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        verify(qcu).txBegin();
        verify(qcu).txCommit();
        verify(qcu, never()).txRollback();
    }

    @Test
    @DisplayName("send: putMessage TlqException returns SendResult.fail (no throw) and rolls back tx")
    void send_putMessageFails_shouldRollbackAndReturnFail() throws TlqException {
        final TlqMessage msg = realtimeSendMsg("M2");
        when(channelMapper.toQueueType(any())).thenReturn(QueueType.REALTIME_SEND);
        when(resolver.resolve(any())).thenReturn("QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        when(mapper.toSdkMessage(msg)).thenReturn(mock(com.tongtech.tlq.base.TlqMessage.class));
        final TlqException tle = mock(TlqException.class);
        when(tle.getErrorCause()).thenReturn("queue full");
        doThrow(tle).when(qcu).putMessage(any(), any(TlqMsgOpt.class));
        when(errorMapper.mapCause("queue full")).thenReturn(FepErrorCode.TRANS_7003);

        final SendResult result = producer.send(msg);

        assertThat(result.success()).isFalse();
        assertThat(result.msgId()).isEqualTo("M2");
        assertThat(result.error()).startsWith("TRANS_7003:");
        assertThat(result.error()).contains("queue full");
        verify(qcu).txRollback();
    }

    @Test
    @DisplayName("send: rollback failure does not mask original error — still returns SendResult.fail")
    void send_rollbackAlsoFails_shouldStillReturnFail() throws TlqException {
        final TlqMessage msg = realtimeSendMsg("M3");
        when(channelMapper.toQueueType(any())).thenReturn(QueueType.REALTIME_SEND);
        when(resolver.resolve(any())).thenReturn("QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        when(mapper.toSdkMessage(msg)).thenReturn(mock(com.tongtech.tlq.base.TlqMessage.class));
        final TlqException putEx = mock(TlqException.class);
        when(putEx.getErrorCause()).thenReturn("network");
        doThrow(putEx).when(qcu).putMessage(any(), any(TlqMsgOpt.class));
        final TlqException rollbackEx = mock(TlqException.class);
        when(rollbackEx.getErrorCause()).thenReturn("rollback failed");
        doThrow(rollbackEx).when(qcu).txRollback();
        when(errorMapper.mapCause("network")).thenReturn(FepErrorCode.TRANS_7003);

        final SendResult result = producer.send(msg);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).startsWith("TRANS_7003:");
        assertThat(result.error()).contains("network");
    }

    @Test
    @DisplayName("send: lazy connect when not yet connected (acceptance #6)")
    void send_lazyConnect_whenNotConnected() throws TlqException {
        when(factory.isConnected()).thenReturn(false);
        final TlqMessage msg = realtimeSendMsg("M4");
        when(channelMapper.toQueueType(any())).thenReturn(QueueType.REALTIME_SEND);
        when(resolver.resolve(any())).thenReturn("QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1");
        when(mapper.toSdkMessage(msg)).thenReturn(mock(com.tongtech.tlq.base.TlqMessage.class));

        final SendResult result = producer.send(msg);

        verify(factory).connect();
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("send: connect failure throws FepBusinessException(TRANS_7002) — not retryable at producer layer")
    void send_connectFails_shouldThrow() {
        when(factory.isConnected()).thenReturn(false);
        doThrow(new FepBusinessException(FepErrorCode.TRANS_7002, "connect failed"))
                .when(factory).connect();
        final TlqMessage msg = realtimeSendMsg("M5");

        assertThatThrownBy(() -> producer.send(msg))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.TRANS_7002);
    }
}
