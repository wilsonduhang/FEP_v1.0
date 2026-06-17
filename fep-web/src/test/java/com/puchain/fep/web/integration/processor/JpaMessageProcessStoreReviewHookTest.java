package com.puchain.fep.web.integration.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * §5.8 审核旁路集成验证：{@link JpaMessageProcessStore#updateStatus} 在
 * {@code FAILED + PROC_8507} 时创建审核任务；其余情况不创建（Plan Task 3 / D4）。
 *
 * <p><strong>非事务测试</strong>：审核任务经 {@code REQUIRES_NEW} 独立事务提交（best-effort
 * 语义），@{@code Transactional} 回滚无法清除，故本类非事务 + {@code @BeforeEach} 按 id 前缀
 * 显式清理共享 H2（红线 shared_h2_..._test_isolation）。测试 id 前缀 {@code rec-h}（非 hex，
 * 不与真实 UUID 记录碰撞）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class JpaMessageProcessStoreReviewHookTest {

    @Autowired
    private MessageProcessStore store;

    @Autowired
    private MessageReviewTaskRepository reviewRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM message_review_task WHERE message_record_id LIKE 'rec-h%'");
        jdbc.update("DELETE FROM message_process_record WHERE id LIKE 'rec-h%'");
    }

    private static MessageType msg1001() {
        return MessageType.byMsgNo("1001").orElseThrow();
    }

    @Test
    void updateStatus_failedWithProc8507_createsReviewTask() {
        store.save(MessageProcessRecord.initial("rec-h1", msg1001(), "txn-h1", Instant.now()));

        store.updateStatus("rec-h1", MessageProcessStatus.FAILED,
                FepErrorCode.PROC_8507.getCode(), "biz rule X");

        assertThat(reviewRepo.findByMessageRecordId("rec-h1")).hasValueSatisfying(t -> {
            assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
            assertThat(t.getErrorCode()).isEqualTo(FepErrorCode.PROC_8507.getCode());
            assertThat(t.getViolationSummary()).isEqualTo("biz rule X");
            assertThat(t.getTransitionNo()).isEqualTo("txn-h1");
        });
    }

    @Test
    void updateStatus_failedWithXsd8501_noReviewTask() {
        store.save(MessageProcessRecord.initial("rec-h2", msg1001(), "txn-h2", Instant.now()));

        store.updateStatus("rec-h2", MessageProcessStatus.FAILED,
                FepErrorCode.PROC_8501.getCode(), "xsd err");

        assertThat(reviewRepo.findByMessageRecordId("rec-h2")).isEmpty();
    }

    @Test
    void updateStatus_completed_noReviewTask() {
        store.save(MessageProcessRecord.initial("rec-h3", msg1001(), "txn-h3", Instant.now()));

        store.updateStatus("rec-h3", MessageProcessStatus.COMPLETED, null, null);

        assertThat(reviewRepo.findByMessageRecordId("rec-h3")).isEmpty();
    }
}
