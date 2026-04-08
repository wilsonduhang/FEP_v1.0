package com.puchain.fep.web.bizdata.record.domain;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
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
 * Entity for business message record, mapped to {@code t_biz_message_record}.
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_biz_message_record")
@EntityListeners(AuditingEntityListener.class)
public class BizMessageRecord {

    /** Primary key (UUID 32). */
    @Id
    @Column(name = "record_id", length = 32)
    private String recordId;

    /** Message code (e.g. "3000"). */
    @Column(name = "message_code", nullable = false, length = 10)
    private String messageCode;

    /** Serial number (unique). */
    @Column(name = "serial_no", nullable = false, length = 50)
    private String serialNo;

    /** Sender node code. */
    @Column(name = "sender_node", length = 20)
    private String senderNode;

    /** Receiver node code. */
    @Column(name = "receiver_node", length = 20)
    private String receiverNode;

    /** Message direction (OUTBOUND/INBOUND/BIDIRECTIONAL). */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private MessageDirection direction;

    /** Processing status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private MessageProcessStatus processStatus;

    /** Business number (optional). */
    @Column(name = "business_no", length = 100)
    private String businessNo;

    /** Transaction amount (optional). */
    @Column(name = "amount", precision = 18, scale = 4)
    private BigDecimal amount;

    /** Raw XML content. */
    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    /** How this record was created. */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_method", nullable = false, length = 20)
    private EntryMethod entryMethod;

    /** Number of times this record has been accessed. */
    @Column(name = "access_count", nullable = false)
    private long accessCount;

    /** Error message when processing fails (nullable). */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** Timestamp when processing completed. */
    @Column(name = "process_time")
    private LocalDateTime processTime;

    /** Created timestamp. */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** Updated timestamp. */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** Default constructor for JPA. */
    public BizMessageRecord() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * Get record ID.
     *
     * @return record ID (UUID 32)
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
     * Get transaction amount.
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

    // ===== Setters =====

    /**
     * Set record ID.
     *
     * @param recordId record ID
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * Set message code.
     *
     * @param messageCode message code
     */
    public void setMessageCode(final String messageCode) {
        this.messageCode = messageCode;
    }

    /**
     * Set serial number.
     *
     * @param serialNo serial number
     */
    public void setSerialNo(final String serialNo) {
        this.serialNo = serialNo;
    }

    /**
     * Set sender node.
     *
     * @param senderNode sender node
     */
    public void setSenderNode(final String senderNode) {
        this.senderNode = senderNode;
    }

    /**
     * Set receiver node.
     *
     * @param receiverNode receiver node
     */
    public void setReceiverNode(final String receiverNode) {
        this.receiverNode = receiverNode;
    }

    /**
     * Set message direction.
     *
     * @param direction direction enum
     */
    public void setDirection(final MessageDirection direction) {
        this.direction = direction;
    }

    /**
     * Set processing status.
     *
     * @param processStatus process status enum
     */
    public void setProcessStatus(final MessageProcessStatus processStatus) {
        this.processStatus = processStatus;
    }

    /**
     * Set business number.
     *
     * @param businessNo business number
     */
    public void setBusinessNo(final String businessNo) {
        this.businessNo = businessNo;
    }

    /**
     * Set transaction amount.
     *
     * @param amount amount
     */
    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Set XML content.
     *
     * @param xmlContent XML content
     */
    public void setXmlContent(final String xmlContent) {
        this.xmlContent = xmlContent;
    }

    /**
     * Set entry method.
     *
     * @param entryMethod entry method enum
     */
    public void setEntryMethod(final EntryMethod entryMethod) {
        this.entryMethod = entryMethod;
    }

    /**
     * Set access count.
     *
     * @param accessCount access count
     */
    public void setAccessCount(final long accessCount) {
        this.accessCount = accessCount;
    }

    /**
     * Set error message.
     *
     * @param errorMessage error message
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Set process time.
     *
     * @param processTime process time
     */
    public void setProcessTime(final LocalDateTime processTime) {
        this.processTime = processTime;
    }
}
