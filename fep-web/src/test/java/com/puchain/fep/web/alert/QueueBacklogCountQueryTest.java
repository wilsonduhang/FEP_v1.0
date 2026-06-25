package com.puchain.fep.web.alert;

import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.outbound.consumer.OutboundQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackQueueRepository#countBacklog()} /
 * {@link OutboundQueueRepository#countBacklog()} 真 SQL 正确性（DEF-B9-3 T1）。
 *
 * <p>断言积压计数只含 {@code PENDING} 与到期 {@code RETRY}（{@code next_retry_at<=now}），
 * 排除未来 RETRY / {@code SENDING} / {@code DEAD_LETTER}。</p>
 *
 * <p><b>隔离策略</b>：全部 {@code @SpringBootTest} 共享同一 in-JVM H2，{@code countBacklog()}
 * 是<b>全表</b> COUNT，易被 sibling 已提交行污染（红线 shared_h2_topn_aggregation_test_isolation）。
 * 故用 <b>delta 断言</b>：{@code @Transactional} 内先取 baseline，再插入 5 行各状态，断言增量
 * 恰为 2（PENDING+due-RETRY），方法结束回滚清理。delta 免疫既有行、无需全表 DELETE、无 FK 风险。
 * （实施期偏离签字 Plan 的「非事务 + @BeforeEach DELETE」：全表 COUNT 断言用 delta 更稳健，
 * 见 commit 披露。）{@code @SpringBootTest} 而非 {@code @DataJpaTest} 因 Flyway 全量 schema 需完整
 * 上下文（镜像 {@code OutboundQueueRepositoryIntegrationTest}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("QueueBacklogCount: countBacklog 仅数 PENDING + 到期 RETRY")
class QueueBacklogCountQueryTest {

    private static final String CALLBACK_INSERT = """
        INSERT INTO callback_queue (queue_id, idempotency_key, target_interface_id, msg_no,
            payload_json, status, retry_count, next_retry_at, create_time, update_time)
        VALUES (?, ?, 'IF_CB_01', '3009', '{}', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

    private static final String OUTBOUND_INSERT = """
        INSERT INTO outbound_message_queue (queue_id, message_type, transition_no, idempotency_key,
            message_head_xml, message_body_xml, payload_data_type, status, retry_count,
            next_retry_at, created_at, updated_at)
        VALUES (?, '3009', ?, ?, '<H/>', '<B/>', 'INVOICE_RETURN_3009', ?, ?, ?,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

    @Autowired
    private CallbackQueueRepository callbackRepo;
    @Autowired
    private OutboundQueueRepository outboundRepo;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void callbackCountBacklog_countsPendingAndDueRetryOnly() {
        final long base = callbackRepo.countBacklog();
        final Timestamp due = Timestamp.from(Instant.now().minusSeconds(60));
        final Timestamp future = Timestamp.from(Instant.now().plusSeconds(3600));

        insertCallback("cbac1111bbbb2222cccc3333dddd0001", "cbk-idem-0001", "PENDING", 0, null);
        insertCallback("cbac1111bbbb2222cccc3333dddd0002", "cbk-idem-0002", "RETRY", 1, due);
        insertCallback("cbac1111bbbb2222cccc3333dddd0003", "cbk-idem-0003", "RETRY", 1, future);
        insertCallback("cbac1111bbbb2222cccc3333dddd0004", "cbk-idem-0004", "SENDING", 0, null);
        insertCallback("cbac1111bbbb2222cccc3333dddd0005", "cbk-idem-0005", "DEAD_LETTER", 5, null);

        assertThat(callbackRepo.countBacklog()).isEqualTo(base + 2);
    }

    @Test
    void outboundCountBacklog_countsPendingAndDueRetryOnly() {
        final long base = outboundRepo.countBacklog();
        final Timestamp due = Timestamp.from(Instant.now().minusSeconds(60));
        final Timestamp future = Timestamp.from(Instant.now().plusSeconds(3600));

        insertOutbound("obac1111bbbb2222cccc3333dddd0001", "10000001", "obk-idem-0001", "PENDING", 0, null);
        insertOutbound("obac1111bbbb2222cccc3333dddd0002", "10000002", "obk-idem-0002", "RETRY", 1, due);
        insertOutbound("obac1111bbbb2222cccc3333dddd0003", "10000003", "obk-idem-0003", "RETRY", 1, future);
        insertOutbound("obac1111bbbb2222cccc3333dddd0004", "10000004", "obk-idem-0004", "SENDING", 0, null);
        insertOutbound("obac1111bbbb2222cccc3333dddd0005", "10000005", "obk-idem-0005", "DEAD_LETTER", 5, null);

        assertThat(outboundRepo.countBacklog()).isEqualTo(base + 2);
    }

    private void insertCallback(final String queueId, final String idem, final String status,
            final int retryCount, final Timestamp nextRetryAt) {
        jdbc.update(CALLBACK_INSERT, queueId, idem, status, retryCount, nextRetryAt);
    }

    private void insertOutbound(final String queueId, final String transitionNo, final String idem,
            final String status, final int retryCount, final Timestamp nextRetryAt) {
        jdbc.update(OUTBOUND_INSERT, queueId, transitionNo, idem, status, retryCount, nextRetryAt);
    }
}
