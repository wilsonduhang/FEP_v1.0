package com.puchain.fep.web.integration.processor;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.web.audit.review.service.MessageReviewTaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p><strong>§5.8 审核旁路（additive, best-effort）</strong>：{@link #updateStatus} 在报文落
 * {@link MessageProcessStatus#FAILED} 且 {@code errorCode == PROC_8507}（业务规则违规）时，
 * 额外创建一条人工审核任务（{@link MessageReviewTaskService#createFromFailedRecord}，
 * {@code REQUIRES_NEW} 独立事务）。该旁路<strong>不改变</strong>状态机流转与既有持久化语义——
 * 报文仍照常落终态 FAILED；审核任务创建失败仅记 WARN，<strong>不</strong>回滚报文 FAILED
 * （中转 liveness 不变量优先，muzhou 2026-06-17 决策）。XSD 结构失败（{@code PROC_8501}）与
 * 非失败态不触发审核；Batch 通路经 {@code transition(...FAILED)} 不携带 errorCode，结构上不进入
 * 本旁路（Plan D5：Batch 审核 deferred）。</p>
 */
@Component("jpaMessageProcessStore")
@Primary
public class JpaMessageProcessStore implements MessageProcessStore {

    private static final Logger log = LoggerFactory.getLogger(JpaMessageProcessStore.class);

    private final MessageProcessRecordJpaRepository repository;
    private final MessageReviewTaskService reviewTaskService;

    /**
     * Creates the adapter with the required Spring Data repository and the
     * §5.8 review-task service used by the additive PROC_8507 hook.
     *
     * @param repository        Spring Data JPA repository bean, non-null
     * @param reviewTaskService 审核任务服务（业务规则失败旁路），non-null
     */
    public JpaMessageProcessStore(final MessageProcessRecordJpaRepository repository,
                                  final MessageReviewTaskService reviewTaskService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.reviewTaskService = Objects.requireNonNull(reviewTaskService, "reviewTaskService");
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
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "recordId passed through LogSanitizer.sanitize() prior to LOG")
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
        final MessageProcessRecordEntity savedEntity = repository.save(entity);
        if (newStatus == MessageProcessStatus.FAILED
                && FepErrorCode.PROC_8507.getCode().equals(errorCode)) {
            // §5.8 旁路（best-effort）：业务规则失败报文额外建一条人工审核任务。
            // createFromFailedRecord 为 REQUIRES_NEW 独立事务——其失败（极罕见并发唯一冲突）
            // 不回滚本方法的报文 FAILED 落库；报文必达终态优先（muzhou 2026-06-17 决策）。
            try {
                reviewTaskService.createFromFailedRecord(
                        savedEntity.getId(), savedEntity.getMessageType(),
                        savedEntity.getTransitionNo(), errorCode, savedEntity.getErrorMessage());
            } catch (RuntimeException ex) {
                log.warn("review task creation failed (best-effort) for recordId={}; "
                        + "FAILED state persisted regardless", LogSanitizer.sanitize(id), ex);
            }
        }
        return toDomain(savedEntity);
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
