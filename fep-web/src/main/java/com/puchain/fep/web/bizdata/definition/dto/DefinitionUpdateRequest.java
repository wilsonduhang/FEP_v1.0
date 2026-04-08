package com.puchain.fep.web.bizdata.definition.dto;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a business message definition.
 *
 * <p>All fields are optional; only non-null values will be applied.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DefinitionUpdateRequest {

    /** Message code (1-5 digit number, optional). */
    @Pattern(regexp = "\\d{1,5}", message = "报文编码为 1-5 位数字")
    private String messageCode;

    /** Message name (optional). */
    @Size(min = 2, max = 200, message = "报文名称长度 2-200 字符")
    private String messageName;

    /** Message direction (optional). */
    private MessageDirection direction;

    /** Associated business type ID (optional). */
    private String businessTypeId;

    /** Number of fields (optional). */
    private Integer fieldCount;

    /** Field summary (optional). */
    private String fieldSummary;

    /** Sample XML (optional). */
    private String sampleXml;

    /** Sort order (optional). */
    private Integer sortOrder;

    /**
     * Get message code.
     *
     * @return message code (may be null)
     */
    public String getMessageCode() {
        return messageCode;
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
     * Get message name.
     *
     * @return message name (may be null)
     */
    public String getMessageName() {
        return messageName;
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
     * Get message direction.
     *
     * @return direction (may be null)
     */
    public MessageDirection getDirection() {
        return direction;
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
     * Get business type ID.
     *
     * @return business type ID (may be null)
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * Set business type ID.
     *
     * @param businessTypeId business type ID
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * Get field count.
     *
     * @return field count (may be null)
     */
    public Integer getFieldCount() {
        return fieldCount;
    }

    /**
     * Set field count.
     *
     * @param fieldCount field count
     */
    public void setFieldCount(final Integer fieldCount) {
        this.fieldCount = fieldCount;
    }

    /**
     * Get field summary.
     *
     * @return field summary (may be null)
     */
    public String getFieldSummary() {
        return fieldSummary;
    }

    /**
     * Set field summary.
     *
     * @param fieldSummary field summary
     */
    public void setFieldSummary(final String fieldSummary) {
        this.fieldSummary = fieldSummary;
    }

    /**
     * Get sample XML.
     *
     * @return sample XML (may be null)
     */
    public String getSampleXml() {
        return sampleXml;
    }

    /**
     * Set sample XML.
     *
     * @param sampleXml sample XML
     */
    public void setSampleXml(final String sampleXml) {
        this.sampleXml = sampleXml;
    }

    /**
     * Get sort order.
     *
     * @return sort order (may be null)
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * Set sort order.
     *
     * @param sortOrder sort order
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
