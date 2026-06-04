package com.puchain.fep.web.requeststate;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Listener behaviour for {@link RequestStateInboundListener}: inbound 结果写点。
 *
 * <p>通过 Spring {@link ApplicationEventPublisher} 真实发布
 * {@link InboundMessageProcessedEvent} 触发 listener（不绕过 listener 方法语义，遵
 * {@code feedback_unit_test_bypass} 精神）；{@code @SpringBootTest} 与本包
 * {@link RequestStateServiceTest} 一致（H2 MODE=MySQL DDL 需完整 Flyway + 应用上下文）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("RequestStateInboundListener: 匹配标记 RESULT_RECEIVED + unmatched 静默不抛")
class RequestStateInboundListenerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private RequestStateService service;

    @Autowired
    private RequestStateRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void matchedInbound_marksResultReceived_fillsInboundFields() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3101, "00000001", "SERIAL-X", null, Instant.now()));

        final RequestStateEntity entity =
                repository.findByCorrelationKey("00000001").orElseThrow();
        assertThat(entity.getLifecycleStatus())
                .isEqualTo(RequestStateLifecycle.RESULT_RECEIVED);
        assertThat(entity.getInboundSerialNo()).isEqualTo("SERIAL-X");
        assertThat(entity.getInboundTransitionNo()).isEqualTo("00000001");
        assertThat(entity.getResultReceivedAt()).isNotNull();
    }

    @Test
    void unmatchedInbound_doesNotThrow_andLeavesOtherRowsUntouched() {
        // pre-existing unrelated row that must stay in SENT
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        assertThatCode(() -> publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3101, "99999999", "SERIAL-Y", null, Instant.now())))
                .doesNotThrowAnyException();

        // no row created for the unmatched correlation key
        assertThat(repository.findByCorrelationKey("99999999")).isEmpty();
        // the unrelated row is untouched (still SENT, no inbound fields)
        final RequestStateEntity untouched =
                repository.findByCorrelationKey("00000001").orElseThrow();
        assertThat(untouched.getLifecycleStatus()).isEqualTo(RequestStateLifecycle.SENT);
        assertThat(untouched.getInboundSerialNo()).isNull();
    }
}
