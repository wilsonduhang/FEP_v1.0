package com.puchain.fep.web.integration.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA entity for the {@code corporate_account_records} table created by Flyway
 * migration {@code V40__create_corporate_account_records.sql} (§6.4.1
 * FR-DATA-DB-01, PRD v1.3 §1958 对公账户信息表).
 *
 * <p>Tracks one row per corporate account ({@code enterprise_id} =
 * {@code QyAccQueryReturn3006.qyAccCode}, the USCI), upserted on each status
 * query回执. {@code account_status} holds the <em>raw</em> HNDEMP return code
 * ({@code accReturnCode}); semantic ENUM mapping is DEFERRED (see {@code DEF-B2-2}).
 * {@code account_number} / {@code opening_bank} / {@code account_type} are
 * nullable — the 3006 回执 carries none of them. Mirrors
 * {@link com.puchain.fep.web.integration.reconciliation.ReconciliationRecordEntity}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "corporate_account_records")
public class CorporateAccountRecordEntity {

    @Id
    @Column(name = "enterprise_id", length = 64, nullable = false)
    private String enterpriseId;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "account_number", length = 32)
    private String accountNumber;

    @Column(name = "opening_bank", length = 100)
    private String openingBank;

    @Column(name = "account_type", length = 20)
    private String accountType;

    @Column(name = "account_status", length = 20, nullable = false)
    private String accountStatus;

    @Column(name = "status_memo", length = 200)
    private String statusMemo;

    @Column(name = "last_verification_time", nullable = false)
    private LocalDateTime lastVerificationTime;

    @Column(name = "serial_no", length = 64, nullable = false)
    private String serialNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public CorporateAccountRecordEntity() {
        // JPA
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} when not explicitly set, since
     * V40 SQL has no DEFAULT clause for these timestamps.
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

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(final String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOpeningBank() {
        return openingBank;
    }

    public void setOpeningBank(final String openingBank) {
        this.openingBank = openingBank;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(final String accountType) {
        this.accountType = accountType;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(final String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getStatusMemo() {
        return statusMemo;
    }

    public void setStatusMemo(final String statusMemo) {
        this.statusMemo = statusMemo;
    }

    public LocalDateTime getLastVerificationTime() {
        return lastVerificationTime;
    }

    public void setLastVerificationTime(final LocalDateTime lastVerificationTime) {
        this.lastVerificationTime = lastVerificationTime;
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
