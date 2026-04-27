package com.puchain.fep.web.integration.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapping for the {@code reconciliation_records} table created by
 * Flyway migration {@code V18__create_reconciliation_tables.sql}.
 *
 * <p>13 columns: 7 PRD (PRD v1.3 §1983) + 6 P2e extensions
 * ({@code paired_serial_no} / {@code discrepancy_count} / {@code reconciliation_time} /
 * {@code created_at} / {@code updated_at} + {@code reconciliation_date} as {@link LocalDate}).</p>
 *
 * <p>{@link PrePersist} / {@link PreUpdate} fall back to fill {@code created_at} /
 * {@code updated_at} since V18 schema has no SQL DEFAULT (Task 2 reviewer Q2).
 * Domain invariants live in {@link com.puchain.fep.processor.reconciliation.ReconciliationRecord}
 * (immutable POJO); this class is a plain mutable JPA POJO.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "reconciliation_records")
public class ReconciliationRecordEntity {

    @Id
    @Column(name = "reconciliation_id", length = 32, nullable = false)
    private String reconciliationId;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "message_type", length = 4, nullable = false)
    private String messageType;

    @Column(name = "serial_no", length = 64, nullable = false)
    private String serialNo;

    @Column(name = "paired_serial_no", length = 64)
    private String pairedSerialNo;

    @Column(name = "total_transaction_count", nullable = false)
    private Integer totalTransactionCount;

    @Column(name = "total_transaction_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalTransactionAmount;

    @Column(name = "actual_count", nullable = false)
    private Integer actualCount;

    @Column(name = "reconciliation_status", length = 20, nullable = false)
    private String reconciliationStatus;

    @Column(name = "discrepancy_count", nullable = false)
    private Integer discrepancyCount;

    @Column(name = "reconciliation_time", nullable = false)
    private LocalDateTime reconciliationTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public ReconciliationRecordEntity() {
        // JPA
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} when not explicitly set, since
     * V18 SQL has no DEFAULT clause for these timestamps.
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

    public String getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(final String reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(final LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String serialNo) {
        this.serialNo = serialNo;
    }

    public String getPairedSerialNo() {
        return pairedSerialNo;
    }

    public void setPairedSerialNo(final String pairedSerialNo) {
        this.pairedSerialNo = pairedSerialNo;
    }

    public Integer getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public void setTotalTransactionCount(final Integer totalTransactionCount) {
        this.totalTransactionCount = totalTransactionCount;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public void setTotalTransactionAmount(final BigDecimal totalTransactionAmount) {
        this.totalTransactionAmount = totalTransactionAmount;
    }

    public Integer getActualCount() {
        return actualCount;
    }

    public void setActualCount(final Integer actualCount) {
        this.actualCount = actualCount;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(final String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public Integer getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(final Integer discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public LocalDateTime getReconciliationTime() {
        return reconciliationTime;
    }

    public void setReconciliationTime(final LocalDateTime reconciliationTime) {
        this.reconciliationTime = reconciliationTime;
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
