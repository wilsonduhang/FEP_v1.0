package com.puchain.fep.web.requeststate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service behaviour for {@link RequestStateService}: 5-state single-writer machine.
 *
 * <p>使用 {@code @SpringBootTest} 与本包 {@link RequestStateRepositoryTest} 一致（H2 MODE=MySQL DDL
 * 需完整 Flyway + 应用上下文），默认 {@code dev} profile。</p>
 *
 * <p><b>无 {@code @Transactional}</b>：被测 {@link RequestStateService} 写方法是
 * {@code REQUIRES_NEW}（best-effort 隔离），会真提交逃逸测试回滚，故不能靠测试事务回滚清理——
 * 改用物理 {@code @BeforeEach}/{@code @AfterEach deleteAll()} 双端清理（镜像
 * {@code JpaOutboundMessageEnqueueServiceTest}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("RequestStateService: CREATED→SENT→RESULT_RECEIVED + 幂等 + 非法转换防御 + unmatched 信号")
class RequestStateServiceTest {

    @Autowired
    private RequestStateService service;

    @Autowired
    private RequestStateRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void create_nonBlockedType_setsCreatedRow_correlationBlockedFalse() {
        service.create("00000001", "3101", "QID-1");

        final Optional<RequestStateEntity> found = repository.findByCorrelationKey("00000001");
        assertThat(found).isPresent();
        assertThat(found.get().getLifecycleStatus()).isEqualTo(RequestStateLifecycle.CREATED);
        assertThat(found.get().getMessageType()).isEqualTo("3101");
        assertThat(found.get().getOutboundQueueId()).isEqualTo("QID-1");
        assertThat(found.get().isCorrelationBlocked()).isFalse();
    }

    @Test
    void create_blockedType3115_setsCorrelationBlockedTrue() {
        service.create("00000002", "3115", "QID-2");

        final RequestStateEntity entity =
                repository.findByCorrelationKey("00000002").orElseThrow();
        assertThat(entity.isCorrelationBlocked()).isTrue();
    }

    @Test
    void create_normalizesCorrelationKey_trimsWhitespace() {
        service.create("  00000003  ", "3101", "QID-3");

        assertThat(repository.findByCorrelationKey("00000003")).isPresent();
    }

    @Test
    void markSent_movesCreatedToSent_recordsSentAt() {
        service.create("00000001", "3101", "QID-1");

        final boolean updated = service.markSent("00000001");

        assertThat(updated).isTrue();
        final RequestStateEntity entity =
                repository.findByCorrelationKey("00000001").orElseThrow();
        assertThat(entity.getLifecycleStatus()).isEqualTo(RequestStateLifecycle.SENT);
        assertThat(entity.getSentAt()).isNotNull();
    }

    @Test
    void markResultReceived_recordsInboundFields_andResultReceivedState() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        final boolean matched =
                service.markResultReceived("00000001", "SERIAL-X", "00000001");

        assertThat(matched).isTrue();
        final RequestStateEntity entity =
                repository.findByCorrelationKey("00000001").orElseThrow();
        assertThat(entity.getLifecycleStatus()).isEqualTo(RequestStateLifecycle.RESULT_RECEIVED);
        assertThat(entity.getInboundSerialNo()).isEqualTo("SERIAL-X");
        assertThat(entity.getInboundTransitionNo()).isEqualTo("00000001");
        assertThat(entity.getResultReceivedAt()).isNotNull();
    }

    @Test
    void markResultReceived_unmatchedCorrelation_returnsFalse_noThrow() {
        final boolean matched =
                service.markResultReceived("99999999", "SERIAL-Y", "99999999");

        assertThat(matched).isFalse();
        assertThat(repository.findByCorrelationKey("99999999")).isEmpty();
    }

    @Test
    void markResultReceived_normalizesCorrelationKey_matchesTrimmed() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        final boolean matched =
                service.markResultReceived("  00000001  ", "SERIAL-Z", "  00000001  ");

        assertThat(matched).isTrue();
        final RequestStateEntity entity =
                repository.findByCorrelationKey("00000001").orElseThrow();
        assertThat(entity.getInboundTransitionNo()).isEqualTo("00000001");
    }

    @Test
    void markFailed_movesToFailed() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        final boolean updated = service.markFailed("00000001");

        assertThat(updated).isTrue();
        assertThat(repository.findByCorrelationKey("00000001").orElseThrow()
                .getLifecycleStatus()).isEqualTo(RequestStateLifecycle.FAILED);
    }

    @Test
    void markSent_idempotent_alreadySent_isNoOp_noError() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        // second markSent on an already-SENT row must not throw and must remain SENT
        final boolean second = service.markSent("00000001");

        assertThat(second).isTrue();
        assertThat(repository.findByCorrelationKey("00000001").orElseThrow()
                .getLifecycleStatus()).isEqualTo(RequestStateLifecycle.SENT);
    }

    @Test
    void markStuck_movesSentToStuck() {
        service.create("00000001", "3101", "QID-1");
        service.markSent("00000001");

        final boolean updated = service.markStuck("00000001");

        assertThat(updated).isTrue();
        assertThat(repository.findByCorrelationKey("00000001").orElseThrow()
                .getLifecycleStatus()).isEqualTo(RequestStateLifecycle.STUCK);
    }

    @Test
    void markStuck_unknownCorrelation_returnsFalse_noThrow() {
        assertThat(service.markStuck("99999999")).isFalse();
    }

    @Test
    void markSent_unknownCorrelation_returnsFalse_noThrow() {
        final boolean updated = service.markSent("99999999");

        assertThat(updated).isFalse();
    }

    @Test
    void markFailed_unknownCorrelation_returnsFalse_noThrow() {
        assertThat(service.markFailed("99999999")).isFalse();
    }
}
