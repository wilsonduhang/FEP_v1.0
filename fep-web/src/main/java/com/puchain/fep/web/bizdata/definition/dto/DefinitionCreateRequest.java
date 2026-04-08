package com.puchain.fep.web.bizdata.definition.dto;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a business message definition.
 *
 * <p>See PRD v1.3 section 5.3.1 + section 5.3.2.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DefinitionCreateRequest {

    /** Message code (1-5 digit number). */
    @NotBlank(message = "报文编码不能为空")
    @Pattern(regexp = "\\d{1,5}", message = "报文编码为 1-5 位数字")
    private String messageCode;

    /** Message name. */
    @NotBlank(message = "报文名称不能为空")
    @Size(min = 2, max = 200, message = "报文名称长度 2-200 字符")
    private String messageName;

    /** Message direction. */
    @NotNull(message = "报文方向不能为空")
    private MessageDirection direction;

    /** Associated business type ID (optional). */
    private String businessTypeId;

    /** Number of fields. */
    private int fieldCount;

    /** Field summary (optional). */
    private String fieldSummary;

    /** Sample XML (optional). */
    private String sampleXml;

    /** Sort order (optional, default 0). */
    private int sortOrder;

    /**
     * Get message code.
     *
     * @return message code
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
     * @return message name
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
     * @return direction enum
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
     * Get business type ID (may be null).
     *
     * @return business type ID
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
     * @return field count
     */
    public int getFieldCount() {
        return fieldCount;
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
     * Get field summary (may be null).
     *
     * @return field summary
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
     * Get sample XML (may be null).
     *
     * @return sample XML
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
     * @return sort order
     */
    public int getSortOrder() {
        return sortOrder;
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
