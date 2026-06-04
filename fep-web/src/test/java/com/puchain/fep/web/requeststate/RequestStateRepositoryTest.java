package com.puchain.fep.web.requeststate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link RequestStateRepository}.
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL 的 DDL 需要完整
 * Flyway + 应用上下文（与本模块全部 repository test，如 {@code ReconciliationRecordRepositoryTest}
 * / {@code CallbackQueueRepositoryTest} 一致）。不声明 {@code @ActiveProfiles}，走默认 {@code dev}
 * profile。</p>
 *
 * <p>{@code findStuck} 依据 {@code updated_at}，而 entity 的 {@code markXxx} 会把 {@code updated_at}
 * 刷为 {@code Instant.now()}，故测试先 {@code save} 再用 {@link JdbcTemplate} 直接 UPDATE
 * {@code updated_at} 字段模拟历史时间点（镜像 {@code SubSubmissionRecordRepositoryTest} 手法）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("RequestStateRepository: CRUD + findByCorrelationKey + findStuck(排除 blocked)")
class RequestStateRepositoryTest {

    @Autowired
    private RequestStateRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void save_andFindByCorrelationKey_hit() {
        final RequestStateEntity entity =
                RequestStateEntity.created("00000001", "3101", "QID-1", false);
        repository.save(entity);

        final Optional<RequestStateEntity> found = repository.findByCorrelationKey("00000001");

        assertThat(found).isPresent();
        assertThat(found.get().getMessageType()).isEqualTo("3101");
        assertThat(found.get().getOutboundQueueId()).isEqualTo("QID-1");
        assertThat(found.get().getLifecycleStatus()).isEqualTo(RequestStateLifecycle.CREATED);
        assertThat(found.get().isCorrelationBlocked()).isFalse();
    }

    @Test
    void findByCorrelationKey_miss_returnsEmpty() {
        repository.save(RequestStateEntity.created("00000001", "3101", "QID-1", false));

        assertThat(repository.findByCorrelationKey("99999999")).isEmpty();
    }

    @Test
    void findStuck_returnsSentRowsOlderThanThreshold_excludingBlocked() {
        final Instant now = Instant.now();
        final Instant threshold = now.minus(10, ChronoUnit.MINUTES);

        // (a) SENT + stale + not-blocked  -> should be returned
        final String staleSentId = persistSent("00000001", "3101", false);
        backdateUpdatedAt(staleSentId, now.minus(30, ChronoUnit.MINUTES));

        // (b) SENT + stale + blocked       -> excluded (correlation_blocked = true)
        final String staleBlockedId = persistSent("00000002", "3115", true);
        backdateUpdatedAt(staleBlockedId, now.minus(30, ChronoUnit.MINUTES));

        // (c) SENT + fresh + not-blocked   -> excluded (updatedAt >= threshold)
        final String freshSentId = persistSent("00000003", "3101", false);
        backdateUpdatedAt(freshSentId, now.minus(1, ChronoUnit.MINUTES));

        // (d) CREATED + stale + not-blocked -> excluded (not SENT)
        final RequestStateEntity created =
                RequestStateEntity.created("00000004", "3101", "QID-4", false);
        repository.save(created);
        backdateUpdatedAt(created.getRequestStateId(), now.minus(30, ChronoUnit.MINUTES));

        final List<RequestStateEntity> stuck = repository.findStuck(threshold);

        assertThat(stuck)
                .extracting(RequestStateEntity::getRequestStateId)
                .containsExactly(staleSentId);
        assertThat(stuck)
                .extracting(RequestStateEntity::getRequestStateId)
                .doesNotContain(staleBlockedId, freshSentId, created.getRequestStateId());
    }

    private String persistSent(final String correlationKey, final String messageType,
                               final boolean blocked) {
        final RequestStateEntity entity =
                RequestStateEntity.created(correlationKey, messageType, "QID-" + correlationKey, blocked);
        entity.markSent();
        repository.save(entity);
        return entity.getRequestStateId();
    }

    private void backdateUpdatedAt(final String requestStateId, final Instant updatedAt) {
        repository.flush();
        jdbcTemplate.update("UPDATE t_request_state SET updated_at = ? WHERE request_state_id = ?",
                Timestamp.from(updatedAt), requestStateId);
    }
}
