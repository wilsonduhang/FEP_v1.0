package com.puchain.fep.web.bizdata.definition.domain;

import com.puchain.fep.common.domain.EnableDisableStatus;
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

import java.time.LocalDateTime;

/**
 * Entity for business message definition, mapped to {@code t_biz_message_definition}.
 *
 * <p>See PRD v1.3 section 5.3.1 + section 5.3.2 (FR-WEB-BIZ-LIST, FR-WEB-BIZ-DICT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_biz_message_definition")
@EntityListeners(AuditingEntityListener.class)
public class BizMessageDefinition {

    /** Primary key (UUID 32). */
    @Id
    @Column(name = "definition_id", length = 32)
    private String definitionId;

    /** Message code (1-5 digit number, e.g. "3000"). */
    @Column(name = "message_code", nullable = false, length = 10)
    private String messageCode;

    /** Message name. */
    @Column(name = "message_name", nullable = false, length = 200)
    private String messageName;

    /** Associated business type ID (nullable). */
    @Column(name = "business_type_id", length = 32)
    private String businessTypeId;

    /** Message direction. */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private MessageDirection direction;

    /** Number of fields in the message definition. */
    @Column(name = "field_count", nullable = false)
    private int fieldCount;

    /** Summary of fields (nullable). */
    @Column(name = "field_summary", length = 2000)
    private String fieldSummary;

    /** Sample XML content (nullable). */
    @Column(name = "sample_xml", columnDefinition = "TEXT")
    private String sampleXml;

    /** Definition status (ENABLED / DISABLED). */
    @Enumerated(EnumType.STRING)
    @Column(name = "definition_status", nullable = false, length = 20)
    private EnableDisableStatus definitionStatus;

    /** Sort order. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Created timestamp. */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** Updated timestamp. */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** Default constructor for JPA. */
    public BizMessageDefinition() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * Get definition ID.
     *
     * @return definition ID (UUID 32)
     */
    public String getDefinitionId() {
        return definitionId;
    }

    /**
     * Get message code.
     *
     * @return message code (1-5 digits)
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * Get message name.
     *
     * @return message name
     */
    public String getMessageName() {
        return messageName;
    }

    /**
     * Get business type ID (may be null).
     *
     * @return business type ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
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
     * Get field count.
     *
     * @return field count
     */
    public int getFieldCount() {
        return fieldCount;
    }

    /**
     * Get field summary (may be null).
     *
     * @return field summary
     */
    public String getFieldSummary() {
        return fieldSummary;
    }

    /**
     * Get sample XML (may be null).
     *
     * @return sample XML
     */
    public String getSampleXml() {
        return sampleXml;
    }

    /**
     * Get definition status.
     *
     * @return definition status enum
     */
    public EnableDisableStatus getDefinitionStatus() {
        return definitionStatus;
    }

    /**
     * Get sort order.
     *
     * @return sort order
     */
    public int getSortOrder() {
        return sortOrder;
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
     * Set definition ID.
     *
     * @param definitionId definition ID
     */
    public void setDefinitionId(final String definitionId) {
        this.definitionId = definitionId;
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
     * Set message name.
     *
     * @param messageName message name
     */
    public void setMessageName(final String messageName) {
        this.messageName = messageName;
    }

    /**
     * Set business type ID.
     *
     * @param businessTypeId business type ID (may be null)
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
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
     * Set field count.
     *
     * @param fieldCount field count
     */
    public void setFieldCount(final int fieldCount) {
        this.fieldCount = fieldCount;
    }

    /**
     * Set field summary.
     *
     * @param fieldSummary field summary (may be null)
     */
    public void setFieldSummary(final String fieldSummary) {
        this.fieldSummary = fieldSummary;
    }

    /**
     * Set sample XML.
     *
     * @param sampleXml sample XML (may be null)
     */
    public void setSampleXml(final String sampleXml) {
        this.sampleXml = sampleXml;
    }

    /**
     * Set definition status.
     *
     * @param definitionStatus definition status enum
     */
    public void setDefinitionStatus(final EnableDisableStatus definitionStatus) {
        this.definitionStatus = definitionStatus;
    }

    /**
     * Set sort order.
     *
     * @param sortOrder sort order
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
