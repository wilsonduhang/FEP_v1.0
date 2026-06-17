package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MessageReviewTaskService#createFromFailedRecord} 行为验证（Task 2）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class MessageReviewTaskServiceCreateTest {

    @Autowired
    private MessageReviewTaskService reviewService;

    @Autowired
    private MessageReviewTaskRepository repository;

    @Test
    void create_fromFailedRecord_persistsPendingTask() {
        reviewService.createFromFailedRecord("rec-1", "1001", "txn-1", "PROC_8507", "field X invalid");

        final MessageReviewTaskEntity t = repository.findByMessageRecordId("rec-1").orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
        assertThat(t.getReviewLevel()).isEqualTo(1);
        assertThat(t.getCurrentLevel()).isEqualTo(1);
        assertThat(t.getErrorCode()).isEqualTo("PROC_8507");
        assertThat(t.getViolationSummary()).isEqualTo("field X invalid");
        assertThat(t.getMessageType()).isEqualTo("1001");
        assertThat(t.getReviewerId()).isNull();
        assertThat(t.getReviewedAt()).isNull();
    }

    @Test
    void create_isIdempotent_onDuplicateRecordId() {
        reviewService.createFromFailedRecord("rec-2", "1001", "txn-2", "PROC_8507", "x");
        reviewService.createFromFailedRecord("rec-2", "1001", "txn-2", "PROC_8507", "x");

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void create_withNullViolationSummary_persistsNull() {
        reviewService.createFromFailedRecord("rec-3", "1001", "txn-3", "PROC_8507", null);

        final MessageReviewTaskEntity t = repository.findByMessageRecordId("rec-3").orElseThrow();
        assertThat(t.getViolationSummary()).isNull();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
    }
}
