package com.puchain.fep.web.integration.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity mapping for the {@code clearing_instruction_records} table created by
 * Flyway migration {@code V18__create_reconciliation_tables.sql}.
 *
 * <p>Composite primary key ({@code instruction_id}, {@code qs_serial_no}) is wired
 * via {@link IdClass} pointing to {@link PK}.</p>
 *
 * <p>12 columns: 8 PRD (PRD v1.3 §1995) + 4 P2e extensions ({@code message_id} /
 * {@code instruction_type} / {@code created_at} / {@code updated_at}).
 * {@link PrePersist} / {@link PreUpdate} fill {@code created_at} / {@code updated_at}
 * since V18 schema has no SQL DEFAULT (Task 2 reviewer Q2).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "clearing_instruction_records")
@IdClass(ClearingInstructionRecordEntity.PK.class)
public class ClearingInstructionRecordEntity {

    @Id
    @Column(name = "instruction_id", length = 32, nullable = false)
    private String instructionId;

    @Id
    @Column(name = "qs_serial_no", length = 64, nullable = false)
    private String qsSerialNo;

    @Column(name = "instruction_type", length = 20, nullable = false)
    private String instructionType;

    @Column(name = "settlement_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal settlementAmount;

    @Column(name = "payer_account", length = 64, nullable = false)
    private String payerAccount;

    @Column(name = "payee_account", length = 64, nullable = false)
    private String payeeAccount;

    @Column(name = "instruction_status", length = 20, nullable = false)
    private String instructionStatus;

    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    @Column(name = "failure_cause", length = 200)
    private String failureCause;

    @Column(name = "message_id", length = 32, nullable = false)
    private String messageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public ClearingInstructionRecordEntity() {
        // JPA
    }

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

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(final String instructionId) {
        this.instructionId = instructionId;
    }

    public String getQsSerialNo() {
        return qsSerialNo;
    }

    public void setQsSerialNo(final String qsSerialNo) {
        this.qsSerialNo = qsSerialNo;
    }

    public String getInstructionType() {
        return instructionType;
    }

    public void setInstructionType(final String instructionType) {
        this.instructionType = instructionType;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(final BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public String getPayerAccount() {
        return payerAccount;
    }

    public void setPayerAccount(final String payerAccount) {
        this.payerAccount = payerAccount;
    }

    public String getPayeeAccount() {
        return payeeAccount;
    }

    public void setPayeeAccount(final String payeeAccount) {
        this.payeeAccount = payeeAccount;
    }

    public String getInstructionStatus() {
        return instructionStatus;
    }

    public void setInstructionStatus(final String instructionStatus) {
        this.instructionStatus = instructionStatus;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public String getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(final String failureCause) {
        this.failureCause = failureCause;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
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

    /**
     * Composite primary key for {@link ClearingInstructionRecordEntity}.
     * Required by JPA {@link IdClass}: must be {@link Serializable} with
     * matching field names + {@code equals} / {@code hashCode}.
     */
    public static class PK implements Serializable {

        private static final long serialVersionUID = 1L;

        private String instructionId;
        private String qsSerialNo;

        /**
         * No-arg constructor required by JPA.
         */
        public PK() {
            // JPA
        }

        /**
         * Convenience constructor for tests / adapters.
         *
         * @param instructionId clearing instruction id
         * @param qsSerialNo    clearing serial number
         */
        public PK(final String instructionId, final String qsSerialNo) {
            this.instructionId = instructionId;
            this.qsSerialNo = qsSerialNo;
        }

        public String getInstructionId() {
            return instructionId;
        }

        public void setInstructionId(final String instructionId) {
            this.instructionId = instructionId;
        }

        public String getQsSerialNo() {
            return qsSerialNo;
        }

        public void setQsSerialNo(final String qsSerialNo) {
            this.qsSerialNo = qsSerialNo;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PK other)) {
                return false;
            }
            return Objects.equals(instructionId, other.instructionId)
                    && Objects.equals(qsSerialNo, other.qsSerialNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instructionId, qsSerialNo);
        }
    }
}
