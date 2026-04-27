package com.puchain.fep.processor.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 对账记录 POJO（v1b: 完全不可变，对齐 P2a {@code MessageProcessRecord} 模式）。
 *
 * <p>所有字段 {@code final}，构造器全字段必传（{@code pairedSerialNo} 可空），
 * 无 setter。Service 修改字段必须用 {@link Builder#from(ReconciliationRecord)}
 * rebuild 整体新实例：</p>
 *
 * <pre>{@code
 *   ReconciliationRecord updated = ReconciliationRecord.builder()
 *           .from(existing)
 *           .status("COMPLETED")
 *           .reconciliationTime(LocalDateTime.now())
 *           .updatedAt(LocalDateTime.now())
 *           .build();
 *   store.save(updated);
 * }</pre>
 *
 * <p>13 字段映射至 {@code reconciliation_records} 表（V18__create_reconciliation_tables.sql）：
 * 7 PRD 字段（PRD v1.3 §1983）+ 6 P2e 扩展（pairedSerialNo / discrepancyCount /
 * reconciliationTime / createdAt / updatedAt + reconciliationDate 升级为 LocalDate）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class ReconciliationRecord {

    private final String reconciliationId;
    private final LocalDate reconciliationDate;
    private final String messageType;
    private final String serialNo;
    private final String pairedSerialNo;
    private final int totalTransactionCount;
    private final BigDecimal totalTransactionAmount;
    private final int actualCount;
    private final String status;
    private final int discrepancyCount;
    private final LocalDateTime reconciliationTime;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ReconciliationRecord(final Builder b) {
        this.reconciliationId = Objects.requireNonNull(b.reconciliationId, "reconciliationId");
        this.reconciliationDate = Objects.requireNonNull(b.reconciliationDate, "reconciliationDate");
        this.messageType = Objects.requireNonNull(b.messageType, "messageType");
        this.serialNo = Objects.requireNonNull(b.serialNo, "serialNo");
        this.pairedSerialNo = b.pairedSerialNo;
        this.totalTransactionCount = b.totalTransactionCount;
        this.totalTransactionAmount = Objects.requireNonNull(b.totalTransactionAmount, "totalTransactionAmount");
        this.actualCount = b.actualCount;
        this.status = Objects.requireNonNull(b.status, "status");
        this.discrepancyCount = b.discrepancyCount;
        this.reconciliationTime = Objects.requireNonNull(b.reconciliationTime, "reconciliationTime");
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(b.updatedAt, "updatedAt");
    }

    public String getReconciliationId() {
        return reconciliationId;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getPairedSerialNo() {
        return pairedSerialNo;
    }

    public int getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public int getActualCount() {
        return actualCount;
    }

    public String getStatus() {
        return status;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public LocalDateTime getReconciliationTime() {
        return reconciliationTime;
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
     * Builder for {@link ReconciliationRecord}. 不可变 POJO 的唯一构造路径。
     */
    public static final class Builder {

        private String reconciliationId;
        private LocalDate reconciliationDate;
        private String messageType;
        private String serialNo;
        private String pairedSerialNo;
        private int totalTransactionCount;
        private BigDecimal totalTransactionAmount;
        private int actualCount;
        private String status;
        private int discrepancyCount;
        private LocalDateTime reconciliationTime;
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
        public Builder from(final ReconciliationRecord r) {
            Objects.requireNonNull(r, "r");
            this.reconciliationId = r.reconciliationId;
            this.reconciliationDate = r.reconciliationDate;
            this.messageType = r.messageType;
            this.serialNo = r.serialNo;
            this.pairedSerialNo = r.pairedSerialNo;
            this.totalTransactionCount = r.totalTransactionCount;
            this.totalTransactionAmount = r.totalTransactionAmount;
            this.actualCount = r.actualCount;
            this.status = r.status;
            this.discrepancyCount = r.discrepancyCount;
            this.reconciliationTime = r.reconciliationTime;
            this.createdAt = r.createdAt;
            this.updatedAt = r.updatedAt;
            return this;
        }

        /**
         * Sets reconciliationId.
         *
         * @param v 32-char UUID, required
         * @return this Builder
         */
        public Builder reconciliationId(final String v) {
            this.reconciliationId = v;
            return this;
        }

        /**
         * Sets reconciliationDate.
         *
         * @param v reconciliation date, required
         * @return this Builder
         */
        public Builder reconciliationDate(final LocalDate v) {
            this.reconciliationDate = v;
            return this;
        }

        /**
         * Sets messageType.
         *
         * @param v HNDEMP message type code (3107 / 3108 / 3116), required
         * @return this Builder
         */
        public Builder messageType(final String v) {
            this.messageType = v;
            return this;
        }

        /**
         * Sets serialNo.
         *
         * @param v business serial number, required
         * @return this Builder
         */
        public Builder serialNo(final String v) {
            this.serialNo = v;
            return this;
        }

        /**
         * Sets pairedSerialNo (optional).
         *
         * @param v paired serial number for 3107 / 3108 traceability, nullable
         * @return this Builder
         */
        public Builder pairedSerialNo(final String v) {
            this.pairedSerialNo = v;
            return this;
        }

        /**
         * Sets totalTransactionCount.
         *
         * @param v declared transaction count
         * @return this Builder
         */
        public Builder totalTransactionCount(final int v) {
            this.totalTransactionCount = v;
            return this;
        }

        /**
         * Sets totalTransactionAmount.
         *
         * @param v declared total amount, required
         * @return this Builder
         */
        public Builder totalTransactionAmount(final BigDecimal v) {
            this.totalTransactionAmount = v;
            return this;
        }

        /**
         * Sets actualCount.
         *
         * @param v actual records observed
         * @return this Builder
         */
        public Builder actualCount(final int v) {
            this.actualCount = v;
            return this;
        }

        /**
         * Sets status.
         *
         * @param v {@code ReconciliationStatus} name, required
         * @return this Builder
         */
        public Builder status(final String v) {
            this.status = v;
            return this;
        }

        /**
         * Sets discrepancyCount.
         *
         * @param v number of discrepancy items
         * @return this Builder
         */
        public Builder discrepancyCount(final int v) {
            this.discrepancyCount = v;
            return this;
        }

        /**
         * Sets reconciliationTime.
         *
         * @param v reconciliation execution timestamp, required
         * @return this Builder
         */
        public Builder reconciliationTime(final LocalDateTime v) {
            this.reconciliationTime = v;
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
         * @return new {@link ReconciliationRecord} instance
         */
        public ReconciliationRecord build() {
            return new ReconciliationRecord(this);
        }
    }
}
