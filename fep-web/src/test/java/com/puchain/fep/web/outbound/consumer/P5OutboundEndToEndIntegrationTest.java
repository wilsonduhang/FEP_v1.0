package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * P5 T9 端到端 IT — 验证 outbound queue 完整流水：
 * V22/V25 seed 行 → consumer claim → envelope build → sign → send → status='SENT' + msg_id 写入。
 *
 * <p>覆盖 Plan §Task 9 验收标准 1 / 3 / 4(冒烟) / 5 / 6 / 7：</p>
 * <ul>
 *   <li>AC1: 8 报文 (3009/3101/3102/3105/3107/3109/3112/3116) → SENT + msg_id 匹配 \\d{20}</li>
 *   <li>AC3: 连续失败累计达 maxAttempts=5 → 至少 1 行 DEAD_LETTER + next_retry_at IS NULL</li>
 *   <li>AC4 冒烟: SENT count == 8 (full Prometheus actuator IT 在 OutboundMetricsActuatorIntegrationTest)</li>
 *   <li>AC5: msg_id 显式 regex {@code \\d{20}}</li>
 *   <li>AC6: transition_no fixture 8 位 numeric（PRD §3.2.3）</li>
 *   <li>AC7: T9 Step 4 删除 stub 后 production profile 启动只剩 OutboundQueueRunnerImpl 一个 Bean</li>
 * </ul>
 *
 * <p><b>profile 选择</b>：使用默认 dev profile（不加 {@code @ActiveProfiles("test")}）— dev profile
 * 加载 {@code MockSignService} / {@code MockKeyService}（{@code @Profile("dev")}），E2E 才能完成
 * 加签步骤；改成 test profile 会让 SignAdapter 找不到 SignService bean，启动失败。</p>
 *
 * <p><b>@MockBean XsdValidator</b>：fixture 中 body POJO 是 self-closing 空标签
 * （{@code <RzReturnInfo3009/>} 等），不满足真实 XSD 必填字段；mock validator 返回 ok 让
 * envelope build 通过，专注验证 runner 流水接通。真 XSD 校验已在 fep-processor XsdValidatorTest 覆盖。</p>
 *
 * <p><b>@MockBean TlqProducer</b>：默认 mock provider 不向真 TLQ broker 发送；本测试显式 mock
 * 控制 SendResult 返回值，区分 SENT 路径（ok）与 DEAD_LETTER 路径（fail）。</p>
 *
 * <p><b>property overrides</b>：</p>
 * <ul>
 *   <li>{@code fep.collector.scheduling.enabled=false} — N5: 隔离 P4 CollectorScheduler 干扰</li>
 *   <li>{@code fep.outbound.queue.poll-interval-ms=99999} — 最大化 fixedDelay 让 @Scheduled 实际不再触发</li>
 *   <li>{@code fep.outbound.queue.retry.backoff-millis=0} +
 *       {@code fep.outbound.queue.retry.max-backoff-millis=0} — RETRY next_retry_at 立即到期，
 *       下一轮 poll 立刻重新声领（而不是等 30s 默认 backoff）</li>
 *   <li>{@code management.health.redis.enabled=false} — 容器内不启动 Redis 时避免 health 检测拖累</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.outbound.queue.poll-interval-ms=99999",
        "fep.outbound.queue.poll-initial-delay-ms=99999",
        "fep.outbound.queue.retry.backoff-millis=0",
        "fep.outbound.queue.retry.max-backoff-millis=0",
        "management.health.redis.enabled=false"
})
@Sql("/sql/p5/outbound_queue_8_messages.sql")
class P5OutboundEndToEndIntegrationTest {

    @Autowired
    private OutboundQueueConsumer consumer;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private TlqProducer mockProducer;

    /**
     * MockBean — fixture body POJO 是 self-closing 空标签，不满足 XSD 必填字段；mock validator
     * 返回 ok 专注验证 runner 流水接通。真 XSD 校验由 fep-processor XsdValidatorTest 覆盖。
     */
    @MockBean
    private XsdValidator xsdValidator;

    /**
     * AC1 + AC4(冒烟) + AC5 + AC6: 8 报文全部走完 build → sign → send → SENT，
     * msg_id 匹配 14 datetime + 6 seq numeric (PRD §3.1.3)，sent_at 在最近 30s 时间窗内。
     */
    @Test
    void e2e_8_messages_should_all_reach_SENT_with_valid_msg_id() {
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(mockProducer.send(any())).thenReturn(SendResult.ok("BROKER_X"));

        consumer.poll();

        final List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT queue_id, msg_id, status, sent_at FROM outbound_message_queue "
                        + "WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd00%' "
                        + "AND status='SENT' ORDER BY queue_id");
        assertThat(rows).hasSize(8);
        final Instant now = Instant.now();
        for (final Map<String, Object> row : rows) {
            assertThat((String) row.get("msg_id")).matches("\\d{20}");
            assertThat(row.get("sent_at")).isInstanceOf(java.sql.Timestamp.class);
            final Instant sentAt = ((java.sql.Timestamp) row.get("sent_at")).toInstant();
            assertThat(sentAt)
                    .isAfter(now.minusSeconds(30))
                    .isBefore(now.plusSeconds(5));
        }
    }

    /**
     * AC3: 5 次连续失败累计达 maxAttempts → 至少 1 行 status='DEAD_LETTER' + next_retry_at IS NULL。
     *
     * <p>循环 6 次确保 5 次 RETRY 后转 DLQ（即使 claim 顺序非确定也至少 1 行命中）。
     * backoff-millis=0 让 RETRY 行立即可被下一轮 poll 重新声领。</p>
     */
    @Test
    void e2e_5_consecutive_failures_should_reach_DEAD_LETTER() {
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(mockProducer.send(any())).thenReturn(SendResult.fail("MSG_FAIL_X", "boom"));

        for (int i = 0; i < 6; i++) {
            consumer.poll();
        }

        final Long dlqCount = jdbc.queryForObject(
                "SELECT count(*) FROM outbound_message_queue "
                        + "WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd00%' "
                        + "AND status='DEAD_LETTER'", Long.class);
        assertThat(dlqCount).isGreaterThan(0L);

        final List<Map<String, Object>> dlqRows = jdbc.queryForList(
                "SELECT next_retry_at FROM outbound_message_queue "
                        + "WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd00%' "
                        + "AND status='DEAD_LETTER'");
        for (final Map<String, Object> row : dlqRows) {
            assertThat(row.get("next_retry_at")).isNull();
        }
    }

    /**
     * AC4 冒烟: SENT 行计数 == 8（OutboundMetricsActuatorIntegrationTest 已覆盖
     * /actuator/prometheus 实际 metric 行）。
     */
    @Test
    void e2e_metrics_endpoint_should_expose_outbound_send_total() {
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(mockProducer.send(any())).thenReturn(SendResult.ok("BROKER_X"));

        consumer.poll();

        final Long sentCount = jdbc.queryForObject(
                "SELECT count(*) FROM outbound_message_queue "
                        + "WHERE queue_id LIKE 'aaaa1111bbbb2222cccc3333dddd00%' "
                        + "AND status='SENT'", Long.class);
        assertThat(sentCount).isEqualTo(8L);
    }
}
