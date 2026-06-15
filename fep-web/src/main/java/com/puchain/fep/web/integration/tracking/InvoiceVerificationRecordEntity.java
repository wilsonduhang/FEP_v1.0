package com.puchain.fep.web.integration.tracking;

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
 * JPA entity mapping for the {@code invoice_verification_records} table created
 * by Flyway migration {@code V38__create_invoice_verification_records.sql}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1970 发票核验记录表).
 *
 * <p>8 PRD columns (invoice_id / invoice_code / invoice_number / invoice_amount /
 * invoice_date / verification_result / verification_time / failure_reason) + 3
 * non-PRD extensions ({@code serial_no} idempotency key + {@code created_at} /
 * {@code updated_at} audit), mirroring
 * {@link com.puchain.fep.web.integration.reconciliation.ReconciliationRecordEntity}.</p>
 *
 * <p>{@code verification_result} holds the <em>raw</em> HNDEMP return code
 * ({@code InvoCheckReturn3008.invoCheckReturnCode}); semantic ENUM mapping is
 * DEFERRED to the domain expert (see {@code DEF-B2-2}). {@link PrePersist} /
 * {@link PreUpdate} fall back to fill the audit timestamps since the V38 schema
 * has no SQL DEFAULT. Plain mutable JPA POJO.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "invoice_verification_records")
public class InvoiceVerificationRecordEntity {

    @Id
    @Column(name = "invoice_id", length = 32, nullable = false)
    private String invoiceId;

    @Column(name = "invoice_code", length = 12)
    private String invoiceCode;

    @Column(name = "invoice_number", length = 8)
    private String invoiceNumber;

    @Column(name = "invoice_amount", precision = 20, scale = 4)
    private BigDecimal invoiceAmount;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "verification_result", length = 20, nullable = false)
    private String verificationResult;

    @Column(name = "verification_time", nullable = false)
    private LocalDateTime verificationTime;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "serial_no", length = 64, nullable = false)
    private String serialNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public InvoiceVerificationRecordEntity() {
        // JPA
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} when not explicitly set, since
     * V38 SQL has no DEFAULT clause for these timestamps.
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

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(final String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(final String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public BigDecimal getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(final BigDecimal invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(final LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(final String verificationResult) {
        this.verificationResult = verificationResult;
    }

    public LocalDateTime getVerificationTime() {
        return verificationTime;
    }

    public void setVerificationTime(final LocalDateTime verificationTime) {
        this.verificationTime = verificationTime;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
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
