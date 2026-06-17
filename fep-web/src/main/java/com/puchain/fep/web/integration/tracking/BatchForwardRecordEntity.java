package com.puchain.fep.web.integration.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA entity mapping for the {@code batch_forward_records} table created by
 * Flyway migration {@code V41__create_batch_forward_records.sql}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §2020 非实时业务转发记录表).
 *
 * <p>8 PRD columns (batch_forward_id / batch_type / total_record_count /
 * success_record_count / process_start_time / process_end_time / batch_status /
 * error_log_path) + 3 non-PRD extensions ({@code serial_no} idempotency key +
 * {@code created_at} / {@code updated_at} audit), mirroring
 * {@link InvoiceVerificationRecordEntity}.</p>
 *
 * <p>{@code batch_type} holds the raw derived value (message {@code msgNo}) and
 * {@code batch_status} the raw state-machine state name; semantic ENUM mapping is
 * DEFERRED to the domain expert (see {@code DEF-B2-2}). {@code error_log_path}
 * stays {@code null} (FEP does not persist batch error-log files). {@link PrePersist} /
 * {@link PreUpdate} fall back to fill the audit timestamps since the V41 schema
 * has no SQL DEFAULT. Plain mutable JPA POJO.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "batch_forward_records")
public class BatchForwardRecordEntity {

    @Id
    @Column(name = "batch_forward_id", length = 32, nullable = false)
    private String batchForwardId;

    @Column(name = "batch_type", length = 20, nullable = false)
    private String batchType;

    @Column(name = "total_record_count", nullable = false)
    private int totalRecordCount;

    @Column(name = "success_record_count", nullable = false)
    private int successRecordCount;

    @Column(name = "process_start_time", nullable = false)
    private LocalDateTime processStartTime;

    @Column(name = "process_end_time")
    private LocalDateTime processEndTime;

    @Column(name = "batch_status", length = 20, nullable = false)
    private String batchStatus;

    @Column(name = "error_log_path", length = 200)
    private String errorLogPath;

    @Column(name = "serial_no", length = 64, nullable = false)
    private String serialNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public BatchForwardRecordEntity() {
        // JPA
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} when not explicitly set, since
     * V41 SQL has no DEFAULT clause for these timestamps.
     */
    @PrePersist
    void onCreate() {
        final LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Refreshes {@code updatedAt} on every JPA update.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getBatchForwardId() {
        return batchForwardId;
    }

    public void setBatchForwardId(final String batchForwardId) {
        this.batchForwardId = batchForwardId;
    }

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(final String batchType) {
        this.batchType = batchType;
    }

    public int getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(final int totalRecordCount) {
        this.totalRecordCount = totalRecordCount;
    }

    public int getSuccessRecordCount() {
        return successRecordCount;
    }

    public void setSuccessRecordCount(final int successRecordCount) {
        this.successRecordCount = successRecordCount;
    }

    public LocalDateTime getProcessStartTime() {
        return processStartTime;
    }

    public void setProcessStartTime(final LocalDateTime processStartTime) {
        this.processStartTime = processStartTime;
    }

    public LocalDateTime getProcessEndTime() {
        return processEndTime;
    }

    public void setProcessEndTime(final LocalDateTime processEndTime) {
        this.processEndTime = processEndTime;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(final String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getErrorLogPath() {
        return errorLogPath;
    }

    public void setErrorLogPath(final String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String serialNo) {
        this.serialNo = serialNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
