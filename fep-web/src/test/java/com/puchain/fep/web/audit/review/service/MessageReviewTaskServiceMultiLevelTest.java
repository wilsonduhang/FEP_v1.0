package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 多级审核扩展点验证（{@code fep.review.levels=2}）：逐级推进 currentLevel，达到 reviewLevel 才终结。
 * 证明配置项 {@code fep.review.levels} 真实驱动多级行为（非死代码预留）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = "fep.review.levels=2")
class MessageReviewTaskServiceMultiLevelTest {

    @Autowired
    private MessageReviewTaskService service;

    @Autowired
    private MessageReviewTaskRepository repository;

    @BeforeEach
    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void twoLevel_firstApproveStaysPending_secondApproves() {
        service.createFromFailedRecord("rec-ml1", "1001", "txn-ml1", "PROC_8507", "v");
        final String id = repository.findByMessageRecordId("rec-ml1").orElseThrow().getReviewId();

        // create-side: reviewLevel snapshot from config = 2, currentLevel starts at 1
        MessageReviewTaskEntity t = repository.findById(id).orElseThrow();
        assertThat(t.getReviewLevel()).isEqualTo(2);
        assertThat(t.getCurrentLevel()).isEqualTo(1);

        // L1 approve: advances to level 2, stays PENDING
        service.approve(id, "reviewer-L1", null);
        t = repository.findById(id).orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
        assertThat(t.getCurrentLevel()).isEqualTo(2);

        // L2 approve: reaches reviewLevel, terminal APPROVED
        service.approve(id, "reviewer-L2", "final ok");
        t = repository.findById(id).orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED.name());
        assertThat(t.getReviewerId()).isEqualTo("reviewer-L2");
        assertThat(t.getReviewedAt()).isNotNull();
    }
}
