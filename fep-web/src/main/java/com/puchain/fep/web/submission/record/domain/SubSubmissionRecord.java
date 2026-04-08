package com.puchain.fep.web.submission.record.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报送记录 Entity，映射 t_sub_submission_record 表。
 *
 * <p>参见 PRD v1.3 §5.5.5 报文数据列表 + §5.6 报送管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sub_submission_record")
@EntityListeners(AuditingEntityListener.class)
public class SubSubmissionRecord {

    /** 记录唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "record_id", length = 32)
    private String recordId;

    /** 报文号（如 3101, 1101）。 */
    @Column(name = "message_type", nullable = false, length = 10)
    private String messageType;

    /** 报文名称。 */
    @Column(name = "message_name", nullable = false, length = 200)
    private String messageName;

    /** 关联业务类型 ID（可为 null）。 */
    @Column(name = "business_type_id", length = 32)
    private String businessTypeId;

    /** 报送单位名称。 */
    @Column(name = "submitter_name", length = 200)
    private String submitterName;

    /** 业务编号。 */
    @Column(name = "business_no", length = 100)
    private String businessNo;

    /** 金额（万元）。 */
    @Column(name = "amount", precision = 18, scale = 4)
    private BigDecimal amount;

    /** 数据条数。 */
    @Column(name = "data_count", nullable = false)
    private int dataCount;

    /** 录入方式。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_method", nullable = false, length = 20)
    private EntryMethod entryMethod;

    /** 录入人。 */
    @Column(name = "entry_by", length = 50)
    private String entryBy;

    /** 推送状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false, length = 20)
    private PushStatus pushStatus;

    /** 推送时间。 */
    @Column(name = "push_time")
    private LocalDateTime pushTime;

    /** 推送失败原因。 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 排序数值。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public SubSubmissionRecord() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取记录唯一标识。
     *
     * @return 记录 ID (UUID 32位)
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * 获取报文号。
     *
     * @return 报文号
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * 获取报文名称。
     *
     * @return 报文名称
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * 获取关联业务类型 ID（可为 null）。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取报送单位名称。
     *
     * @return 报送单位名称
     */
    public String getSubmitterName() {
        return submitterName;
    }

    /**
     * 获取业务编号。
     *
     * @return 业务编号
     */
    public String getBusinessNo() {
        return businessNo;
    }

    /**
     * 获取金额（万元）。
     *
     * @return 金额
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 获取数据条数。
     *
     * @return 数据条数
     */
    public int getDataCount() {
        return dataCount;
    }

    /**
     * 获取录入方式。
     *
     * @return 录入方式枚举
     */
    public EntryMethod getEntryMethod() {
        return entryMethod;
    }

    /**
     * 获取录入人。
     *
     * @return 录入人
     */
    public String getEntryBy() {
        return entryBy;
    }

    /**
     * 获取推送状态。
     *
     * @return 推送状态枚举
     */
    public PushStatus getPushStatus() {
        return pushStatus;
    }

    /**
     * 获取推送时间（可为 null）。
     *
     * @return 推送时间
     */
    public LocalDateTime getPushTime() {
        return pushTime;
    }

    /**
     * 获取推送失败原因（可为 null）。
     *
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取排序数值。
     *
     * @return 排序数值
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置记录唯一标识。
     *
     * @param recordId 记录 ID
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * 设置报文号。
     *
     * @param messageType 报文号
     */
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    /**
     * 设置报文名称。
     *
     * @param messageName 报文名称
     */
    public void setMessageName(final String messageName) {
        this.messageName = messageName;
    }

    /**
     * 设置关联业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID（可为 null）
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 设置报送单位名称。
     *
     * @param submitterName 报送单位名称
     */
    public void setSubmitterName(final String submitterName) {
        this.submitterName = submitterName;
    }

    /**
     * 设置业务编号。
     *
     * @param businessNo 业务编号
     */
    public void setBusinessNo(final String businessNo) {
        this.businessNo = businessNo;
    }

    /**
     * 设置金额（万元）。
     *
     * @param amount 金额
     */
    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * 设置数据条数。
     *
     * @param dataCount 数据条数
     */
    public void setDataCount(final int dataCount) {
        this.dataCount = dataCount;
    }

    /**
     * 设置录入方式。
     *
     * @param entryMethod 录入方式枚举
     */
    public void setEntryMethod(final EntryMethod entryMethod) {
        this.entryMethod = entryMethod;
    }

    /**
     * 设置录入人。
     *
     * @param entryBy 录入人
     */
    public void setEntryBy(final String entryBy) {
        this.entryBy = entryBy;
    }

    /**
     * 设置推送状态。
     *
     * @param pushStatus 推送状态枚举
     */
    public void setPushStatus(final PushStatus pushStatus) {
        this.pushStatus = pushStatus;
    }

    /**
     * 设置推送时间。
     *
     * @param pushTime 推送时间（可为 null）
     */
    public void setPushTime(final LocalDateTime pushTime) {
        this.pushTime = pushTime;
    }

    /**
     * 设置推送失败原因。
     *
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 设置排序数值。
     *
     * @param sortOrder 排序数值
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
