package com.puchain.fep.web.integration.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.web.audit.review.service.MessageReviewTaskService;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * §5.8 审核旁路 best-effort 语义验证（muzhou 2026-06-17 决策）：审核任务创建抛异常时，
 * {@link JpaMessageProcessStore#updateStatus} 仍照常落库报文 FAILED 终态（不回滚）。
 *
 * <p>用 {@code @MockBean} 让 {@link MessageReviewTaskService#createFromFailedRecord} 抛异常，
 * 验证 {@code updateStatus} 内 try-catch 吞掉旁路失败、报文 FAILED 不受影响。非事务 + 前缀清理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class JpaMessageProcessStoreReviewHookFailureTest {

    @Autowired
    private MessageProcessStore store;

    @Autowired
    private MessageProcessRecordJpaRepository recordRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private MessageReviewTaskService reviewTaskService;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM message_process_record WHERE id LIKE 'rec-f%'");
    }

    @Test
    void hookFailure_doesNotRollbackFailedPersist() {
        willThrow(new RuntimeException("boom")).given(reviewTaskService)
                .createFromFailedRecord(any(), any(), any(), any(), any());
        store.save(MessageProcessRecord.initial("rec-f1",
                MessageType.byMsgNo("1001").orElseThrow(), "txn-f1", Instant.now()));

        assertThatCode(() -> store.updateStatus("rec-f1", MessageProcessStatus.FAILED,
                FepErrorCode.PROC_8507.getCode(), "biz rule X")).doesNotThrowAnyException();

        assertThat(recordRepo.findById("rec-f1")).hasValueSatisfying(e ->
                assertThat(e.getStatus()).isEqualTo(MessageProcessStatus.FAILED.name()));
    }
}
