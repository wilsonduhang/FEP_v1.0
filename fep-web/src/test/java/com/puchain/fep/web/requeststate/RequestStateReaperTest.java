package com.puchain.fep.web.requeststate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behaviour for {@link RequestStateReaper}: SENT-past-TTL STUCK detection excluding blocked rows.
 *
 * <p><b>无 {@code @Transactional}</b>（镜像 {@link RequestStateServiceTest}）：reaper 经
 * {@link RequestStateService#markStuck(String)} 的 {@code REQUIRES_NEW} 真提交逃逸测试回滚，故用
 * 物理 {@code deleteAll()} 双端清理而非测试事务回滚。{@code stuck-ttl} 收敛为 {@code PT1M} 以便用
 * {@link JdbcTemplate} 回写 {@code updated_at} 制造历史时点（镜像
 * {@link RequestStateRepositoryTest} 手法）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "management.prometheus.metrics.export.enabled=true",
        "fep.request-state.stuck-ttl=PT1M"
})
@TestPropertySource(properties = "fep.request-state.reaper.fixed-delay=86400000")
@DisplayName("RequestStateReaper: sweep marks SENT-past-TTL non-blocked rows STUCK + counter; "
        + "excludes blocked + fresh")
class RequestStateReaperTest {

    @Autowired
    private RequestStateReaper reaper;

    @Autowired
    private RequestStateRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeterRegistry registry;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void sweep_marksStaleSentNonBlockedRowStuck_andIncrementsCounter() {
        final double before = stuckCount();

        final String staleSentId = persistSent("00000001", "3101", false);
        backdateUpdatedAt(staleSentId, Instant.now().minus(30, ChronoUnit.MINUTES));

        reaper.sweep();

        assertThat(repository.findById(staleSentId).orElseThrow().getLifecycleStatus())
                .isEqualTo(RequestStateLifecycle.STUCK);
        assertThat(stuckCount()).isEqualTo(before + 1.0);
    }

    @Test
    void sweep_doesNotMarkBlockedRow_eveniIfStale() {
        // 3115 is correlation_blocked -> findStuck excludes it -> must stay SENT, counter unchanged
        final double before = stuckCount();

        final String staleBlockedId = persistSent("00000002", "3115", true);
        backdateUpdatedAt(staleBlockedId, Instant.now().minus(30, ChronoUnit.MINUTES));

        reaper.sweep();

        assertThat(repository.findById(staleBlockedId).orElseThrow().getLifecycleStatus())
                .isEqualTo(RequestStateLifecycle.SENT);
        assertThat(stuckCount()).isEqualTo(before);
    }

    @Test
    void sweep_doesNotMarkFreshSentRow() {
        final double before = stuckCount();

        final String freshSentId = persistSent("00000003", "3101", false);
        // updatedAt within TTL (PT1M) -> not stale
        backdateUpdatedAt(freshSentId, Instant.now());

        reaper.sweep();

        assertThat(repository.findById(freshSentId).orElseThrow().getLifecycleStatus())
                .isEqualTo(RequestStateLifecycle.SENT);
        assertThat(stuckCount()).isEqualTo(before);
    }

    private double stuckCount() {
        final Counter counter = registry.find(RequestStateReaper.COUNTER_STUCK_TOTAL).counter();
        return counter == null ? 0.0 : counter.count();
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
