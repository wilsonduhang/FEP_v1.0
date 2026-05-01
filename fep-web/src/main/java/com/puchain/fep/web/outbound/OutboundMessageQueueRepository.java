package com.puchain.fep.web.outbound;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link OutboundMessageQueueEntity} (P4 T7a).
 *
 * <p>Surface kept minimal: the only mutator is the inherited {@code save},
 * the only finders are pre-flight idempotency check
 * ({@link #existsByIdempotencyKey(String)}) and audit lookup
 * ({@link #findByIdempotencyKey(String)}). Status-based polling for the future
 * P5+ outbound consumer will be added in a separate Task to keep this Adapter
 * focused on enqueue.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface OutboundMessageQueueRepository
        extends JpaRepository<OutboundMessageQueueEntity, String> {

    /**
     * Pre-flight idempotency check before insert. The DB-level
     * {@code uk_outbound_queue_idempotency_key} UNIQUE constraint provides the
     * concurrent-insert race tie-breaker; this method only handles the
     * happy-path detection.
     *
     * @param idempotencyKey 32-char hex from {@code IdempotencyKeyGenerator}
     * @return true when a row already exists for the given key
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Audit lookup used by tests and downstream consumers to reconcile by
     * idempotency key.
     *
     * @param idempotencyKey 32-char hex from {@code IdempotencyKeyGenerator}
     * @return matching entity or empty
     */
    Optional<OutboundMessageQueueEntity> findByIdempotencyKey(String idempotencyKey);
}
