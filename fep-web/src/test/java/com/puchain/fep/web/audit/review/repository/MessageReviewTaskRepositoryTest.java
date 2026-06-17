package com.puchain.fep.web.audit.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link MessageReviewTaskRepository} 行为验证（表 V41 message_review_task）。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，与 fep-web 其余 Flyway
 * 依赖型仓储测试一致（H2 MODE=MySQL 需完整 Flyway + 上下文）。本表无外键，无需 seed 父行。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class MessageReviewTaskRepositoryTest {

    @Autowired
    private MessageReviewTaskRepository repository;

    @Autowired
    private EntityManager entityManager;

    private static MessageReviewTaskEntity newPending(final String recordId, final String txnNo) {
        final MessageReviewTaskEntity t = new MessageReviewTaskEntity();
        t.setReviewId(IdGenerator.uuid32());
        t.setMessageRecordId(recordId);
        t.setMessageType("1001");
        t.setTransitionNo(txnNo);
        t.setErrorCode("PROC_8507");
        t.setViolationSummary("field X invalid");
        t.setReviewStatus(ReviewStatus.PENDING.name());
        t.setReviewLevel(1);
        t.setCurrentLevel(1);
        t.setCreatedAt(Instant.now().toEpochMilli());
        return t;
    }

    @Test
    void save_then_findByStatus_returnsPending() {
        repository.save(newPending("rec-001", "txn-001"));
        entityManager.flush();
        entityManager.clear();

        final Page<MessageReviewTaskEntity> page =
                repository.findByReviewStatus(ReviewStatus.PENDING.name(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTransitionNo()).isEqualTo("txn-001");
        assertThat(page.getContent().get(0).getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
    }

    @Test
    void findByMessageRecordId_returnsTask() {
        repository.save(newPending("rec-lookup", "txn-lookup"));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByMessageRecordId("rec-lookup")).isPresent();
        assertThat(repository.findByMessageRecordId("rec-absent")).isEmpty();
    }

    @Test
    void uniqueMessageRecordId_rejectsDuplicate() {
        repository.saveAndFlush(newPending("rec-dup", "txn-a"));
        assertThatThrownBy(() -> repository.saveAndFlush(newPending("rec-dup", "txn-b")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
