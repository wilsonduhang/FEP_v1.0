package com.puchain.fep.web.collector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the {@code collection_run} table created by Flyway migration
 * {@code V23__create_collection_run.sql} (P4 T8).
 *
 * <p>Persisted by {@link JdbcCollectionRunRecorder} on behalf of fep-collector
 * {@code CollectorScheduler}; consumed by the future T6b {@code CollectionRunController}
 * for the management UI.</p>
 *
 * <p><b>Adapter package placement (Option A):</b> Plan §T8 §2 originally specified
 * "在 fep-collector 模块" but {@link com.puchain.fep.web.config.JpaConfiguration}
 * declares {@code @EnableJpaRepositories(basePackages = "com.puchain.fep.web")}, so the
 * entity must live under the fep-web scan path to be discovered. This follows the T7a
 * precedent ({@code OutboundMessageQueueEntity} in {@code com.puchain.fep.web.outbound}).
 * The cross-module Port contract stays clean: collector depends only on
 * {@code CollectionRunRecorder} (forward-declared in T6a), unaware of the JPA mapping.</p>
 *
 * <p><b>Status string instead of enum:</b> intentional for V1 — the writer is the
 * sole producer of allowed values (mirrors {@code CollectionRunResult.Status} enum
 * names: RUNNING / SUCCESS / PARTIAL / FAILED / SKIPPED), and a typed
 * {@code @Enumerated(EnumType.STRING)} would couple this fep-web entity to the
 * fep-collector module, which is undesirable.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "collection_run")
public class CollectionRunEntity {

    @Id
    @Column(name = "run_id", nullable = false, length = 32)
    private String runId;

    @Column(name = "adapter_id", nullable = false, length = 64)
    private String adapterId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "collected_count", nullable = false)
    private int collectedCount;

    @Column(name = "assembled_count", nullable = false)
    private int assembledCount;

    @Column(name = "submitted_count", nullable = false)
    private int submittedCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "trigger_source", nullable = false, length = 32)
    private String triggerSource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * No-arg constructor required by JPA.
     */
    public CollectionRunEntity() {
        // JPA
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(final String runId) {
        this.runId = runId;
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(final String adapterId) {
        this.adapterId = adapterId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getCollectedCount() {
        return collectedCount;
    }

    public void setCollectedCount(final int collectedCount) {
        this.collectedCount = collectedCount;
    }

    public int getAssembledCount() {
        return assembledCount;
    }

    public void setAssembledCount(final int assembledCount) {
        this.assembledCount = assembledCount;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(final int submittedCount) {
        this.submittedCount = submittedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(final int errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(final String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }
}
