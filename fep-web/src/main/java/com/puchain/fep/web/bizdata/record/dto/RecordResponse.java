package com.puchain.fep.web.bizdata.record.dto;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.BizMessageRecord;
import com.puchain.fep.web.bizdata.record.domain.EntryMethod;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for business message record.
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RecordResponse {

    /** Record ID. */
    private String recordId;

    /** Message code. */
    private String messageCode;

    /** Serial number. */
    private String serialNo;

    /** Sender node. */
    private String senderNode;

    /** Receiver node. */
    private String receiverNode;

    /** Message direction. */
    private MessageDirection direction;

    /** Processing status. */
    private MessageProcessStatus processStatus;

    /** Business number. */
    private String businessNo;

    /** Transaction amount. */
    private BigDecimal amount;

    /** XML content. */
    private String xmlContent;

    /** Entry method. */
    private EntryMethod entryMethod;

    /** Access count. */
    private long accessCount;

    /** Error message. */
    private String errorMessage;

    /** Process time. */
    private LocalDateTime processTime;

    /** Created timestamp. */
    private LocalDateTime createTime;

    /** Updated timestamp. */
    private LocalDateTime updateTime;

    /**
     * Build a response DTO from an entity.
     *
     * @param entity the message record entity
     * @return response DTO
     */
    public static RecordResponse from(final BizMessageRecord entity) {
        RecordResponse resp = new RecordResponse();
        resp.recordId = entity.getRecordId();
        resp.messageCode = entity.getMessageCode();
        resp.serialNo = entity.getSerialNo();
        resp.senderNode = entity.getSenderNode();
        resp.receiverNode = entity.getReceiverNode();
        resp.direction = entity.getDirection();
        resp.processStatus = entity.getProcessStatus();
        resp.businessNo = entity.getBusinessNo();
        resp.amount = entity.getAmount();
        resp.xmlContent = entity.getXmlContent();
        resp.entryMethod = entity.getEntryMethod();
        resp.accessCount = entity.getAccessCount();
        resp.errorMessage = entity.getErrorMessage();
        resp.processTime = entity.getProcessTime();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * Get record ID.
     *
     * @return record ID
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * Get message code.
     *
     * @return message code
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Get serial number.
     *
     * @return serial number
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Get sender node.
     *
     * @return sender node (may be null)
     */
    public String getSenderNode() {
        return senderNode;
    }

    /**
     * Get receiver node.
     *
     * @return receiver node (may be null)
     */
    public String getReceiverNode() {
        return receiverNode;
    }

    /**
     * Get message direction.
     *
     * @return direction enum
     */
    public MessageDirection getDirection() {
        return direction;
    }

    /**
     * Get processing status.
     *
     * @return process status enum
     */
    public MessageProcessStatus getProcessStatus() {
        return processStatus;
    }

    /**
     * Get business number.
     *
     * @return business number (may be null)
     */
    public String getBusinessNo() {
        return businessNo;
    }

    /**
     * Get amount.
     *
     * @return amount (may be null)
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Get XML content.
     *
     * @return XML content (may be null)
     */
    public String getXmlContent() {
        return xmlContent;
    }

    /**
     * Get entry method.
     *
     * @return entry method enum
     */
    public EntryMethod getEntryMethod() {
        return entryMethod;
    }

    /**
     * Get access count.
     *
     * @return access count
     */
    public long getAccessCount() {
        return accessCount;
    }

    /**
     * Get error message.
     *
     * @return error message (may be null)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get process time.
     *
     * @return process time (may be null)
     */
    public LocalDateTime getProcessTime() {
        return processTime;
    }

    /**
     * Get created timestamp.
     *
     * @return create time
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * Get updated timestamp.
     *
     * @return update time
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
