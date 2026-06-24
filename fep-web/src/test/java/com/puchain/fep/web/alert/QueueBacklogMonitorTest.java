package com.puchain.fep.web.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link QueueBacklogMonitor} 边沿触发语义单元测试（DEF-B9-3 T4 核心逻辑）。
 *
 * <p>覆盖：below→above 上穿触发 / 持续 above 不重发 / above→below→above 回落 re-arm 二次触发 /
 * 持续 below 零 / 边界 depth==threshold / 两队列独立状态 / 各 queue 开关 disabled 跳过。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class QueueBacklogMonitorTest {

    private static final long THRESHOLD = 1000L;

    @Mock
    private com.puchain.fep.web.callback.repository.CallbackQueueRepository callbackRepo;
    @Mock
    private com.puchain.fep.web.outbound.consumer.OutboundQueueRepository outboundRepo;
    @Mock
    private ApplicationEventPublisher publisher;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC);

    private QueueBacklogMonitor monitor(final boolean callbackEnabled, final boolean outboundEnabled) {
        final QueueBacklogAlertProperties props = new QueueBacklogAlertProperties(
                true, 60000L, THRESHOLD, callbackEnabled, outboundEnabled);
        return new QueueBacklogMonitor(callbackRepo, outboundRepo, publisher, props, clock);
    }

    @BeforeEach
    void noOutboundByDefault() {
        // 多数用例只关注 callback；outbound 默认返回 0（below），避免噪声触发。
    }

    @Test
    void belowThenAbove_firesOnceOnUpwardCrossing() {
        when(callbackRepo.countBacklog()).thenReturn(500L, 1500L);
        when(outboundRepo.countBacklog()).thenReturn(0L, 0L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan(); // below
        m.scan(); // above -> fire

        final ArgumentCaptor<QueueBacklogEvent> cap = ArgumentCaptor.forClass(QueueBacklogEvent.class);
        verify(publisher, times(1)).publishEvent(cap.capture());
        assertThat(cap.getValue().queue()).isEqualTo(QueueBacklogQueue.CALLBACK);
        assertThat(cap.getValue().backlogDepth()).isEqualTo(1500L);
        assertThat(cap.getValue().threshold()).isEqualTo(THRESHOLD);
    }

    @Test
    void sustainedAbove_firesOnlyOnce() {
        when(callbackRepo.countBacklog()).thenReturn(2000L, 2000L, 2000L);
        when(outboundRepo.countBacklog()).thenReturn(0L, 0L, 0L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan();
        m.scan();
        m.scan();

        verify(publisher, times(1)).publishEvent(any(QueueBacklogEvent.class));
    }

    @Test
    void aboveBelowAbove_firesTwiceAfterReArm() {
        when(callbackRepo.countBacklog()).thenReturn(1500L, 500L, 1500L);
        when(outboundRepo.countBacklog()).thenReturn(0L, 0L, 0L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan(); // above -> fire
        m.scan(); // below -> re-arm
        m.scan(); // above -> fire again

        verify(publisher, times(2)).publishEvent(any(QueueBacklogEvent.class));
    }

    @Test
    void sustainedBelow_neverFires() {
        when(callbackRepo.countBacklog()).thenReturn(100L, 200L);
        when(outboundRepo.countBacklog()).thenReturn(0L, 0L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan();
        m.scan();

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void depthEqualToThreshold_isTreatedAsCrossing() {
        when(callbackRepo.countBacklog()).thenReturn(THRESHOLD);
        when(outboundRepo.countBacklog()).thenReturn(0L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan();

        verify(publisher, times(1)).publishEvent(any(QueueBacklogEvent.class));
    }

    @Test
    void queuesHaveIndependentState() {
        when(callbackRepo.countBacklog()).thenReturn(1500L);
        when(outboundRepo.countBacklog()).thenReturn(10L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan();

        final ArgumentCaptor<QueueBacklogEvent> cap = ArgumentCaptor.forClass(QueueBacklogEvent.class);
        verify(publisher, times(1)).publishEvent(cap.capture());
        assertThat(cap.getValue().queue()).isEqualTo(QueueBacklogQueue.CALLBACK);
    }

    @Test
    void bothQueuesAbove_fireBothIndependently() {
        when(callbackRepo.countBacklog()).thenReturn(1500L);
        when(outboundRepo.countBacklog()).thenReturn(3000L);
        final QueueBacklogMonitor m = monitor(true, true);

        m.scan();

        verify(publisher, times(2)).publishEvent(any(QueueBacklogEvent.class));
    }

    @Test
    void callbackDisabled_skipsCallbackQueue() {
        when(outboundRepo.countBacklog()).thenReturn(0L);
        final QueueBacklogMonitor m = monitor(false, true);

        m.scan();

        verifyNoInteractions(callbackRepo);
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void outboundDisabled_skipsOutboundQueue() {
        when(callbackRepo.countBacklog()).thenReturn(0L);
        final QueueBacklogMonitor m = monitor(true, false);

        m.scan();

        verifyNoInteractions(outboundRepo);
        verify(publisher, never()).publishEvent(any());
    }
}
