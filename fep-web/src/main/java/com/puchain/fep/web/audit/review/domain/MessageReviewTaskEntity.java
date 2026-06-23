package com.puchain.fep.web.audit.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * JPA entity for the {@code message_review_task} table (Flyway {@code V41}).
 *
 * <p>A review task is created (additively, via the
 * {@code JpaMessageProcessStore.updateStatus} hook) whenever a message lands
 * {@code FAILED} with error code {@code PROC_8507} (business-rule violation).
 * The original {@code message_process_record} still transitions to its terminal
 * {@code FAILED} state unchanged — this row is a parallel human-review work item,
 * not a state-machine change.</p>
 *
 * <p>Conventions mirror {@code MessageProcessRecordEntity}: plain mutable POJO
 * (JPA requirement), {@code id} is a 32-char UUID, timestamps are epoch millis.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "message_review_task")
public class MessageReviewTaskEntity {

    @Id
    @Column(name = "review_id", length = 32, nullable = false)
    private String reviewId;

    @Column(name = "message_record_id", length = 32, nullable = false, unique = true)
    private String messageRecordId;

    @Column(name = "message_type", length = 8, nullable = false)
    private String messageType;

    @Column(name = "transition_no", length = 30, nullable = false)
    private String transitionNo;

    @Column(name = "error_code", length = 16, nullable = false)
    private String errorCode;

    @Column(name = "violation_summary", length = 512)
    private String violationSummary;

    @Column(name = "review_status", length = 16, nullable = false)
    private String reviewStatus;

    @Column(name = "review_level", nullable = false)
    private int reviewLevel;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "assigned_reviewer_id", length = 32)
    private String assignedReviewerId;

    @Column(name = "reviewer_id", length = 32)
    private String reviewerId;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "reviewed_at")
    private Long reviewedAt;

    /**
     * 乐观锁版本号（并发审核决策冲突检测，Flyway {@code V44} 的 {@code row_version} 列）。
     *
     * <p>Hibernate 在每次 UPDATE 时自增并以旧值为谓词校验；并发场景下持 stale 版本的写入
     * 命中 0 行 → 抛 {@code ObjectOptimisticLockingFailureException}，杜绝两审核人同时决策
     * 同一 PENDING 任务导致的丢失更新 / 多级 {@code currentLevel} 自增竞争。包装类型以区分
     * 新建（{@code null} → persist 时 Hibernate 置 0）。</p>
     */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long version;

    /**
     * No-arg constructor required by JPA.
     */
    public MessageReviewTaskEntity() {
        // JPA
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(final String reviewId) {
        this.reviewId = reviewId;
    }

    public String getMessageRecordId() {
        return messageRecordId;
    }

    public void setMessageRecordId(final String messageRecordId) {
        this.messageRecordId = messageRecordId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    public String getTransitionNo() {
        return transitionNo;
    }

    public void setTransitionNo(final String transitionNo) {
        this.transitionNo = transitionNo;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    public String getViolationSummary() {
        return violationSummary;
    }

    public void setViolationSummary(final String violationSummary) {
        this.violationSummary = violationSummary;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(final String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public int getReviewLevel() {
        return reviewLevel;
    }

    public void setReviewLevel(final int reviewLevel) {
        this.reviewLevel = reviewLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(final int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getAssignedReviewerId() {
        return assignedReviewerId;
    }

    public void setAssignedReviewerId(final String assignedReviewerId) {
        this.assignedReviewerId = assignedReviewerId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(final String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(final String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(final Long reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }
}
