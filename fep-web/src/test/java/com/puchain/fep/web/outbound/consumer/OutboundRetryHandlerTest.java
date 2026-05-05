package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * P5 T7 集成测试：{@link OutboundRetryHandler} 失败处理 + DLQ exp_backoff。
 *
 * <p>覆盖 4 项验收（Plan §Task 7 §Step 1, lines 1742-1800）：</p>
 * <ol>
 *   <li>retry_count &lt; maxAttempts：{@code status='RETRY'} + {@code next_retry_at = NOW + exp_backoff}</li>
 *   <li>retry_count &ge; maxAttempts：{@code status='DEAD_LETTER'} + {@code next_retry_at=null}</li>
 *   <li>退避上限 cap 在 30 分钟（{@code maxBackoffMillis}）</li>
 *   <li>{@code error_message} 截断到 1024 char</li>
 * </ol>
 *
 * <p><b>SpringBoot 集成层 vs 纯 Mockito 单测的取舍</b>：Plan 测试断言形如
 * {@code repo.findById(queueId).orElseThrow()} —— 期望 round-trip 到 H2，因此
 * 沿用同包 {@code OutboundQueueRepositoryIntegrationTest} 的 {@code @SpringBootTest}
 * + {@code @Transactional} + {@code @Sql} fixture 模式。
 * {@link FixedClockConfig#fixedClockConfiguration()} 通过 {@link Primary @Primary}
 * 覆盖 {@link OutboundConsumerClockConfiguration} 提供的系统时钟，使
 * {@code next_retry_at} 断言可基于固定 {@link #NOW} 推断。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@Sql("/sql/p5/outbound_retry_handler_fixture.sql")
@Import(OutboundRetryHandlerTest.FixedClockConfig.class)
@DisplayName("OutboundRetryHandler: failure handling + exp_backoff + DLQ")
class OutboundRetryHandlerTest {

    /** Fixed reference instant — 与 Plan §Step 1 line 1753 一致。 */
    private static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");

    @Autowired
    private OutboundRetryHandler handler;

    @Autowired
    private OutboundQueueRepository repo;

    @Test
    void handleFailure_retry_count_lt_5_should_set_RETRY_with_exp_backoff() {
        // retry_count=2 → newRetryCount=3 → backoff = min(30000 << 3, 1800000) = 240000ms
        final String queueId = "aaaa1111bbbb2222cccc3333dddd0010";

        handler.handleFailure(queueId, new RuntimeException("boom"));

        final OutboundMessageQueueEntity saved = repo.findById(queueId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("RETRY");
        assertThat(saved.getRetryCount()).isEqualTo(3);
        assertThat(saved.getNextRetryAt()).isEqualTo(NOW.plusMillis(240_000));
        assertThat(saved.getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void handleFailure_retry_count_gte_5_should_set_DEAD_LETTER_and_clear_next_retry_at() {
        final String queueId = "aaaa1111bbbb2222cccc3333dddd0011";

        handler.handleFailure(queueId, new RuntimeException("fatal"));

        final OutboundMessageQueueEntity saved = repo.findById(queueId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(saved.getRetryCount()).isEqualTo(6);
        assertThat(saved.getNextRetryAt()).isNull(); // DLQ 不再调度
        assertThat(saved.getErrorMessage()).isEqualTo("fatal");
    }

    @Test
    void handleFailure_backoff_should_cap_at_30min() {
        // retry_count=3 → newRetryCount=4（仍 < maxAttempts=5）
        // backoff = min(30000 << 4, 1_800_000) = min(480_000, 1_800_000) = 480_000ms
        final String queueId = "aaaa1111bbbb2222cccc3333dddd0012";

        handler.handleFailure(queueId, new RuntimeException("transient"));

        final OutboundMessageQueueEntity saved = repo.findById(queueId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("RETRY");
        assertThat(saved.getRetryCount()).isEqualTo(4);
        assertThat(saved.getNextRetryAt()).isEqualTo(NOW.plusMillis(480_000));
    }

    @Test
    void handleFailure_should_truncate_error_message_at_1024_chars() {
        final String queueId = "aaaa1111bbbb2222cccc3333dddd0013";
        final String longMsg = "x".repeat(2000);

        handler.handleFailure(queueId, new RuntimeException(longMsg));

        final OutboundMessageQueueEntity saved = repo.findById(queueId).orElseThrow();
        assertThat(saved.getErrorMessage()).hasSize(1024);
        assertThat(saved.getErrorMessage()).isEqualTo("x".repeat(1024));
    }

    /**
     * Test-only configuration overriding
     * {@link OutboundConsumerClockConfiguration#systemClock()} with a fixed
     * instant ({@link OutboundRetryHandlerTest#NOW}). Marked {@link Primary}
     * so that {@link OutboundRetryHandler} autowires this clock instead of
     * the system default — making {@code next_retry_at = NOW + backoff}
     * assertions deterministic.
     */
    @TestConfiguration
    static class FixedClockConfig {

        /**
         * @return fixed clock anchored at {@link OutboundRetryHandlerTest#NOW}
         */
        @Bean
        @Primary
        Clock fixedClockConfiguration() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
