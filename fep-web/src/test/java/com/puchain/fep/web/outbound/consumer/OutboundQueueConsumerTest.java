package com.puchain.fep.web.outbound.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 单元测试：{@link OutboundQueueConsumer#poll()} 委派语义 (P5 T2).
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>(a) 每个 claim 出来的 queue_id 都被 {@link OutboundQueueRunner#run(String)} 调用一次</li>
 *   <li>(b) 单行 {@code runner.run()} 抛异常不阻断同批其它行</li>
 *   <li>(c) {@link OutboundQueueRepository#claimBatch(int)} 返回空时不调用 runner</li>
 * </ul>
 *
 * <p>不验证 {@code FOR UPDATE SKIP LOCKED} 语义（属于 Repository 集成测试领地）也不验证
 * {@code @Scheduled} 本身的调度（属于 Spring Framework 行为，无需重复测试）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("OutboundQueueConsumer.poll: per-row dispatch + exception isolation")
class OutboundQueueConsumerTest {

    private OutboundQueueRepository repository;
    private OutboundQueueRunner runner;
    private OutboundQueueProperties props;
    private OutboundQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        repository = mock(OutboundQueueRepository.class);
        runner = mock(OutboundQueueRunner.class);
        props = new OutboundQueueProperties();
        consumer = new OutboundQueueConsumer(repository, runner, props);
    }

    @Test
    void poll_should_invoke_runner_for_each_claimed_id() {
        when(repository.claimBatch(anyInt())).thenReturn(List.of("id1", "id2"));

        consumer.poll();

        final InOrder order = inOrder(runner);
        order.verify(runner).run("id1");
        order.verify(runner).run("id2");
        verify(runner, times(2)).run(anyString());
    }

    @Test
    void poll_should_continue_after_runner_throws_on_first_row() {
        when(repository.claimBatch(anyInt())).thenReturn(List.of("id1", "id2", "id3"));
        doThrow(new RuntimeException("boom")).when(runner).run("id2");

        consumer.poll();

        // All three queue_ids were dispatched; the failure on id2 did not abort the loop.
        verify(runner).run("id1");
        verify(runner).run("id2");
        verify(runner).run("id3");
    }

    @Test
    void poll_should_skip_runner_when_no_ids_claimed() {
        when(repository.claimBatch(anyInt())).thenReturn(List.of());

        consumer.poll();

        verifyNoInteractions(runner);
        verify(repository).claimBatch(anyInt());
        verify(runner, never()).run(anyString());
    }
}
