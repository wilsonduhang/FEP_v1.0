package com.puchain.fep.web.integration.processor;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed {@link MessageProcessStore} adapter for fep-web.
 *
 * <p>Registered under the bean name {@code jpaMessageProcessStore} with
 * {@link Primary}, which satisfies the
 * {@code @ConditionalOnMissingBean(name = "jpaMessageProcessStore")} guard on
 * {@code InMemoryMessageProcessStore} (Task 4) — so when fep-web is on the
 * classpath the JPA adapter fully replaces the in-memory implementation.</p>
 *
 * <p>The adapter translates between the immutable domain record
 * {@link MessageProcessRecord} and the mutable entity
 * {@link MessageProcessRecordEntity}. {@link Instant} values are stored as
 * epoch milliseconds, matching the V16 schema.</p>
 *
 * <p>Transactional boundaries: mutating methods run under the default
 * propagation, read-only lookups use {@code readOnly = true} to enable
 * Hibernate's flush-mode optimisation.</p>
 */
@Component("jpaMessageProcessStore")
@Primary
public class JpaMessageProcessStore implements MessageProcessStore {

    private final MessageProcessRecordJpaRepository repository;

    /**
     * Creates the adapter with the required Spring Data repository.
     *
     * @param repository Spring Data JPA repository bean, non-null
     */
    public JpaMessageProcessStore(final MessageProcessRecordJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional
    public MessageProcessRecord save(final MessageProcessRecord record) {
        Objects.requireNonNull(record, "record");
        final MessageProcessRecordEntity saved = repository.save(toEntity(record));
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageProcessRecord> findById(final String id) {
        Objects.requireNonNull(id, "id");
        return repository.findById(id).map(JpaMessageProcessStore::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageProcessRecord> findByTransitionNo(final String transitionNo) {
        Objects.requireNonNull(transitionNo, "transitionNo");
        final List<MessageProcessRecordEntity> matches = repository.findAllByTransitionNo(transitionNo);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "duplicate transitionNo detected: " + transitionNo + " count=" + matches.size());
        }
        return Optional.of(toDomain(matches.get(0)));
    }

    @Override
    @Transactional
    public MessageProcessRecord updateStatus(final String id,
                                             final MessageProcessStatus newStatus,
                                             final String errorCode,
                                             final String errorMessage) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(newStatus, "newStatus");
        final MessageProcessRecordEntity entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("no record for id=" + id));
        entity.setStatus(newStatus.name());
        entity.setUpdatedAt(Instant.now().toEpochMilli());
        if (newStatus == MessageProcessStatus.FAILED) {
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
        }
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(final MessageProcessStatus status) {
        Objects.requireNonNull(status, "status");
        return repository.countByStatus(status.name());
    }

    private static MessageProcessRecordEntity toEntity(final MessageProcessRecord record) {
        final MessageProcessRecordEntity entity = new MessageProcessRecordEntity();
        entity.setId(record.getId());
        entity.setMessageType(record.getMessageType().msgNo());
        entity.setTransitionNo(record.getTransitionNo());
        entity.setStatus(record.getStatus().name());
        entity.setCreatedAt(record.getCreatedAt().toEpochMilli());
        entity.setUpdatedAt(record.getUpdatedAt().toEpochMilli());
        entity.setErrorCode(record.getErrorCode());
        entity.setErrorMessage(record.getErrorMessage());
        return entity;
    }

    private static MessageProcessRecord toDomain(final MessageProcessRecordEntity entity) {
        final MessageType type = MessageType.byMsgNo(entity.getMessageType())
                .orElseThrow(() -> new IllegalStateException(
                        "unknown messageType code in DB: " + entity.getMessageType()));
        return new MessageProcessRecord(
                entity.getId(),
                type,
                entity.getTransitionNo(),
                MessageProcessStatus.valueOf(entity.getStatus()),
                Instant.ofEpochMilli(entity.getCreatedAt()),
                Instant.ofEpochMilli(entity.getUpdatedAt()),
                entity.getErrorCode(),
                entity.getErrorMessage());
    }
}
