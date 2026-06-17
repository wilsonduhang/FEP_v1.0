package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.dto.ReviewTaskResponse;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link MessageReviewTaskService} 审核决策（list/getById/approve/reject）行为验证（Task 4，单级）。
 *
 * <p>非事务 + {@code deleteAll} 清理（createFromFailedRecord 为 REQUIRES_NEW 独立提交）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class MessageReviewTaskServiceDecisionTest {

    @Autowired
    private MessageReviewTaskService service;

    @Autowired
    private MessageReviewTaskRepository repository;

    @BeforeEach
    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    private String createPending(final String recordId) {
        service.createFromFailedRecord(recordId, "1001", "txn-" + recordId, "PROC_8507", "v");
        return repository.findByMessageRecordId(recordId).orElseThrow().getReviewId();
    }

    @Test
    void approve_singleLevel_marksApproved() {
        final String id = createPending("rec-a1");

        service.approve(id, "reviewer-1", "looks fine");

        final MessageReviewTaskEntity t = repository.findById(id).orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED.name());
        assertThat(t.getReviewerId()).isEqualTo("reviewer-1");
        assertThat(t.getReviewComment()).isEqualTo("looks fine");
        assertThat(t.getReviewedAt()).isNotNull();
        assertThat(t.getCurrentLevel()).isEqualTo(t.getReviewLevel());
    }

    @Test
    void reject_marksRejectedWithReason() {
        final String id = createPending("rec-a2");

        service.reject(id, "reviewer-2", "amount out of range");

        final MessageReviewTaskEntity t = repository.findById(id).orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.REJECTED.name());
        assertThat(t.getReviewComment()).isEqualTo("amount out of range");
        assertThat(t.getReviewedAt()).isNotNull();
    }

    @Test
    void reject_blankReason_throws() {
        final String id = createPending("rec-a3");

        assertThatThrownBy(() -> service.reject(id, "reviewer-2", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approve_alreadyTerminal_throws() {
        final String id = createPending("rec-a4");
        service.approve(id, "reviewer-1", "ok");

        assertThatThrownBy(() -> service.approve(id, "reviewer-1", "again"))
                .isInstanceOf(FepBusinessException.class);
    }

    @Test
    void getById_unknown_throws() {
        assertThatThrownBy(() -> service.getById("no-such-id"))
                .isInstanceOf(FepBusinessException.class);
    }

    @Test
    void list_filtersByStatusAndPagesOneBased() {
        final String id1 = createPending("rec-a5");
        createPending("rec-a6");
        service.approve(id1, "reviewer-1", "ok");

        final PageResult<ReviewTaskResponse> pending = service.list(ReviewStatus.PENDING, 1, 20);
        assertThat(pending.getTotal()).isEqualTo(1);
        assertThat(pending.getPageNum()).isEqualTo(1);
        assertThat(pending.getRecords().get(0).reviewStatus()).isEqualTo(ReviewStatus.PENDING.name());

        final PageResult<ReviewTaskResponse> all = service.list(null, 1, 20);
        assertThat(all.getTotal()).isEqualTo(2);
    }
}
