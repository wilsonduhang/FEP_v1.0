package com.puchain.fep.web.submission.record.dto;

import com.puchain.fep.web.submission.record.domain.EntryMethod;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报送记录响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class SubmissionRecordResponse {

    /** 记录 ID。 */
    private String recordId;

    /** 报文号。 */
    private String messageType;

    /** 报文名称。 */
    private String messageName;

    /** 关联业务类型 ID。 */
    private String businessTypeId;

    /** 报送单位名称。 */
    private String submitterName;

    /** 业务编号。 */
    private String businessNo;

    /** 金额（万元）。 */
    private BigDecimal amount;

    /** 数据条数。 */
    private int dataCount;

    /** 录入方式。 */
    private EntryMethod entryMethod;

    /** 录入人。 */
    private String entryBy;

    /** 推送状态。 */
    private PushStatus pushStatus;

    /** 推送时间。 */
    private LocalDateTime pushTime;

    /** 推送失败原因。 */
    private String errorMessage;

    /** 排序数值。 */
    private int sortOrder;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 报送记录 Entity
     * @return 响应 DTO
     */
    public static SubmissionRecordResponse from(final SubSubmissionRecord entity) {
        SubmissionRecordResponse resp = new SubmissionRecordResponse();
        resp.recordId = entity.getRecordId();
        resp.messageType = entity.getMessageType();
        resp.messageName = entity.getMessageName();
        resp.businessTypeId = entity.getBusinessTypeId();
        resp.submitterName = entity.getSubmitterName();
        resp.businessNo = entity.getBusinessNo();
        resp.amount = entity.getAmount();
        resp.dataCount = entity.getDataCount();
        resp.entryMethod = entity.getEntryMethod();
        resp.entryBy = entity.getEntryBy();
        resp.pushStatus = entity.getPushStatus();
        resp.pushTime = entity.getPushTime();
        resp.errorMessage = entity.getErrorMessage();
        resp.sortOrder = entity.getSortOrder();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取记录 ID。
     *
     * @return 记录 ID
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
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID（可为 null）
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
     * 获取推送时间。
     *
     * @return 推送时间（可为 null）
     */
    public LocalDateTime getPushTime() {
        return pushTime;
    }

    /**
     * 获取推送失败原因。
     *
     * @return 错误信息（可为 null）
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
}
