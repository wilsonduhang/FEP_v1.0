package com.puchain.fep.web.audit.review.repository;

import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link MessageReviewTaskEntity} (table
 * {@code message_review_task}, Flyway V41).
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface MessageReviewTaskRepository
        extends JpaRepository<MessageReviewTaskEntity, String> {

    /**
     * Page review tasks filtered by review status.
     *
     * @param reviewStatus {@link com.puchain.fep.web.audit.review.domain.ReviewStatus} name
     * @param pageable     0-based Spring page request
     * @return page of matching tasks
     */
    Page<MessageReviewTaskEntity> findByReviewStatus(String reviewStatus, Pageable pageable);

    /**
     * Look up a review task by its source message record id (unique).
     *
     * @param messageRecordId source {@code message_process_record.id}
     * @return matching task or empty
     */
    Optional<MessageReviewTaskEntity> findByMessageRecordId(String messageRecordId);
}
