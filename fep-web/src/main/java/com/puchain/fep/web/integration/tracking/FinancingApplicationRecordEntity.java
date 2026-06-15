package com.puchain.fep.web.integration.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for the {@code financing_application_records} table created by
 * Flyway migration {@code V39__create_financing_application_records.sql}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1945 融资申请结果跟踪表).
 *
 * <p>Tracks one row per financing application ({@code application_id} =
 * {@code RzReturnInfo3009.platApplyNo}), upserted as the application progresses
 * through phases. {@code approval_status} holds the <em>raw</em> HNDEMP phase
 * code ({@code rzPhaseCode}); semantic ENUM mapping is DEFERRED (see
 * {@code DEF-B2-2}). {@code enterprise_id} is nullable — 3009 carries no 融资企业
 * USCI, only {@code core_enterprise_name}. Mirrors
 * {@link com.puchain.fep.web.integration.reconciliation.ReconciliationRecordEntity}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "financing_application_records")
public class FinancingApplicationRecordEntity {

    @Id
    @Column(name = "application_id", length = 64, nullable = false)
    private String applicationId;

    @Column(name = "enterprise_id", length = 20)
    private String enterpriseId;

    @Column(name = "core_enterprise_name", length = 200)
    private String coreEnterpriseName;

    @Column(name = "rzpz_no", length = 64)
    private String rzpzNo;

    @Column(name = "application_amount", precision = 20, scale = 4)
    private BigDecimal applicationAmount;

    @Column(name = "approval_amount", precision = 20, scale = 4)
    private BigDecimal approvalAmount;

    @Column(name = "application_time", nullable = false)
    private LocalDateTime applicationTime;

    @Column(name = "approval_status", length = 20, nullable = false)
    private String approvalStatus;

    @Column(name = "result_notice_time")
    private LocalDateTime resultNoticeTime;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "serial_no", length = 64, nullable = false)
    private String serialNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * No-arg constructor required by JPA.
     */
    public FinancingApplicationRecordEntity() {
        // JPA
    }

    /**
     * Fills {@code createdAt} / {@code updatedAt} when not explicitly set, since
     * V39 SQL has no DEFAULT clause for these timestamps.
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

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getCoreEnterpriseName() {
        return coreEnterpriseName;
    }

    public void setCoreEnterpriseName(final String coreEnterpriseName) {
        this.coreEnterpriseName = coreEnterpriseName;
    }

    public String getRzpzNo() {
        return rzpzNo;
    }

    public void setRzpzNo(final String rzpzNo) {
        this.rzpzNo = rzpzNo;
    }

    public BigDecimal getApplicationAmount() {
        return applicationAmount;
    }

    public void setApplicationAmount(final BigDecimal applicationAmount) {
        this.applicationAmount = applicationAmount;
    }

    public BigDecimal getApprovalAmount() {
        return approvalAmount;
    }

    public void setApprovalAmount(final BigDecimal approvalAmount) {
        this.approvalAmount = approvalAmount;
    }

    public LocalDateTime getApplicationTime() {
        return applicationTime;
    }

    public void setApplicationTime(final LocalDateTime applicationTime) {
        this.applicationTime = applicationTime;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(final String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public LocalDateTime getResultNoticeTime() {
        return resultNoticeTime;
    }

    public void setResultNoticeTime(final LocalDateTime resultNoticeTime) {
        this.resultNoticeTime = resultNoticeTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
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
