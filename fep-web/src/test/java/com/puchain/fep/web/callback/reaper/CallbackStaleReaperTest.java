package com.puchain.fep.web.callback.reaper;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试 for {@link CallbackStaleReaper}。
 *
 * <p>fixed {@link Clock} 钉死「现在」为 2026-05-28T12:00:00Z，配 staleAfterSeconds=300 →
 * 阈值确定为 2026-05-28T11:55:00，验证 reaper 以精确阈值查询、对命中行 markAsStaleReclaim +
 * save + counter++，空集合静默不写库。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackStaleReaperTest {

    @Mock
    private CallbackQueueRepository repo;
    @Mock
    private CallbackQueueProperties props;
    @Mock
    private CallbackQueueProperties.Reaper reaperProps;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-28T12:00:00Z"), ZoneOffset.UTC);
    private SimpleMeterRegistry registry;
    private CallbackStaleReaper reaper;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        when(props.reaper()).thenReturn(reaperProps);
        when(reaperProps.staleAfterSeconds()).thenReturn(300L);
        reaper = new CallbackStaleReaper(repo, props, fixedClock, registry);
    }

    @Test
    void reapRevertsStaleSendingRowsAtPreciseThreshold() {
        final CallbackQueueEntity stale = mock(CallbackQueueEntity.class);
        when(repo.findStaleSending(LocalDateTime.parse("2026-05-28T11:55:00")))
                .thenReturn(List.of(stale));

        reaper.reap();

        verify(stale).markAsStaleReclaim();
        verify(repo).save(stale);
        assertThat(registry.counter(CallbackStaleReaper.COUNTER_REVERTED_TOTAL).count())
                .isEqualTo(1.0);
    }

    @Test
    void reapEmptyDoesNothing() {
        when(repo.findStaleSending(any())).thenReturn(List.of());

        reaper.reap();

        verify(repo, never()).save(any());
        assertThat(registry.counter(CallbackStaleReaper.COUNTER_REVERTED_TOTAL).count())
                .isZero();
    }
}
