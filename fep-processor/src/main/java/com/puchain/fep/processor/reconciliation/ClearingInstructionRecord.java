package com.puchain.fep.processor.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 清算指令记录 POJO（v1c: 完全不可变，对齐 P2a {@code MessageProcessRecord} 模式）。
 *
 * <p>所有字段 {@code final}，构造器全字段必传（{@code executionTime} / {@code failureCause}
 * 可空），无 setter。Service 修改字段必须用
 * {@link Builder#from(ClearingInstructionRecord)} rebuild 整体新实例。</p>
 *
 * <p>12 字段映射至 {@code clearing_instruction_records} 表（V18__create_reconciliation_tables.sql）：
 * 8 PRD 字段（PRD v1.3 §1995）+ 4 P2e 扩展（messageId / instructionType /
 * createdAt / updatedAt）。复合主键 ({@code instructionId}, {@code qsSerialNo}) 由
 * {@code ClearingInstructionRecordEntity.PK} 在 fep-web 侧承载。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class ClearingInstructionRecord {

    private final String instructionId;
    private final String qsSerialNo;
    private final String instructionType;
    private final BigDecimal settlementAmount;
    private final String payerAccount;
    private final String payeeAccount;
    private final String instructionStatus;
    private final LocalDateTime executionTime;
    private final String failureCause;
    private final String messageId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ClearingInstructionRecord(final Builder b) {
        this.instructionId = Objects.requireNonNull(b.instructionId, "instructionId");
        this.qsSerialNo = Objects.requireNonNull(b.qsSerialNo, "qsSerialNo");
        this.instructionType = Objects.requireNonNull(b.instructionType, "instructionType");
        this.settlementAmount = Objects.requireNonNull(b.settlementAmount, "settlementAmount");
        this.payerAccount = Objects.requireNonNull(b.payerAccount, "payerAccount");
        this.payeeAccount = Objects.requireNonNull(b.payeeAccount, "payeeAccount");
        this.instructionStatus = Objects.requireNonNull(b.instructionStatus, "instructionStatus");
        this.executionTime = b.executionTime;
        this.failureCause = b.failureCause;
        this.messageId = Objects.requireNonNull(b.messageId, "messageId");
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(b.updatedAt, "updatedAt");
    }

    public String getInstructionId() {
        return instructionId;
    }

    public String getQsSerialNo() {
        return qsSerialNo;
    }

    public String getInstructionType() {
        return instructionType;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public String getPayerAccount() {
        return payerAccount;
    }

    public String getPayeeAccount() {
        return payeeAccount;
    }

    public String getInstructionStatus() {
        return instructionStatus;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public String getFailureCause() {
        return failureCause;
    }

    public String getMessageId() {
        return messageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 创建 Builder。
     *
     * @return 全新空白 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ClearingInstructionRecord}. 不可变 POJO 的唯一构造路径。
     */
    public static final class Builder {

        private String instructionId;
        private String qsSerialNo;
        private String instructionType;
        private BigDecimal settlementAmount;
        private String payerAccount;
        private String payeeAccount;
        private String instructionStatus;
        private LocalDateTime executionTime;
        private String failureCause;
        private String messageId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private Builder() {
            // package-private via builder() factory
        }

        /**
         * 从既有记录复制全部字段，便于 rebuild 语义（对齐 P2a {@code MessageProcessRecord}）。
         *
         * @param r 源记录，非空
         * @return 当前 Builder（链式）
         */
        public Builder from(final ClearingInstructionRecord r) {
            Objects.requireNonNull(r, "r");
            this.instructionId = r.instructionId;
            this.qsSerialNo = r.qsSerialNo;
            this.instructionType = r.instructionType;
            this.settlementAmount = r.settlementAmount;
            this.payerAccount = r.payerAccount;
            this.payeeAccount = r.payeeAccount;
            this.instructionStatus = r.instructionStatus;
            this.executionTime = r.executionTime;
            this.failureCause = r.failureCause;
            this.messageId = r.messageId;
            this.createdAt = r.createdAt;
            this.updatedAt = r.updatedAt;
            return this;
        }

        /**
         * Sets instructionId.
         *
         * @param v 32-char UUID, required
         * @return this Builder
         */
        public Builder instructionId(final String v) {
            this.instructionId = v;
            return this;
        }

        /**
         * Sets qsSerialNo.
         *
         * @param v clearing serial number, required
         * @return this Builder
         */
        public Builder qsSerialNo(final String v) {
            this.qsSerialNo = v;
            return this;
        }

        /**
         * Sets instructionType.
         *
         * @param v NORMAL / ERROR_HANDLING / BUSINESS_CANCEL, required
         * @return this Builder
         */
        public Builder instructionType(final String v) {
            this.instructionType = v;
            return this;
        }

        /**
         * Sets settlementAmount.
         *
         * @param v settlement amount, required
         * @return this Builder
         */
        public Builder settlementAmount(final BigDecimal v) {
            this.settlementAmount = v;
            return this;
        }

        /**
         * Sets payerAccount.
         *
         * @param v payer account number, required
         * @return this Builder
         */
        public Builder payerAccount(final String v) {
            this.payerAccount = v;
            return this;
        }

        /**
         * Sets payeeAccount.
         *
         * @param v payee account number, required
         * @return this Builder
         */
        public Builder payeeAccount(final String v) {
            this.payeeAccount = v;
            return this;
        }

        /**
         * Sets instructionStatus.
         *
         * @param v PENDING / PROCESSING / SUCCESS / FAILED, required
         * @return this Builder
         */
        public Builder instructionStatus(final String v) {
            this.instructionStatus = v;
            return this;
        }

        /**
         * Sets executionTime (optional).
         *
         * @param v execution timestamp, nullable until SUCCESS / FAILED
         * @return this Builder
         */
        public Builder executionTime(final LocalDateTime v) {
            this.executionTime = v;
            return this;
        }

        /**
         * Sets failureCause (optional).
         *
         * @param v failure description, nullable for non-FAILED states
         * @return this Builder
         */
        public Builder failureCause(final String v) {
            this.failureCause = v;
            return this;
        }

        /**
         * Sets messageId.
         *
         * @param v inbound message process record id, required
         * @return this Builder
         */
        public Builder messageId(final String v) {
            this.messageId = v;
            return this;
        }

        /**
         * Sets createdAt.
         *
         * @param v created timestamp, required
         * @return this Builder
         */
        public Builder createdAt(final LocalDateTime v) {
            this.createdAt = v;
            return this;
        }

        /**
         * Sets updatedAt.
         *
         * @param v updated timestamp, required
         * @return this Builder
         */
        public Builder updatedAt(final LocalDateTime v) {
            this.updatedAt = v;
            return this;
        }

        /**
         * Builds the immutable record.
         *
         * @return new {@link ClearingInstructionRecord} instance
         */
        public ClearingInstructionRecord build() {
            return new ClearingInstructionRecord(this);
        }
    }
}
