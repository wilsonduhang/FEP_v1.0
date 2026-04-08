package com.puchain.fep.web.bizdata.definition.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.bizdata.definition.domain.BizMessageDefinition;
import com.puchain.fep.web.bizdata.domain.MessageDirection;

import java.time.LocalDateTime;

/**
 * Response DTO for business message definition.
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DefinitionResponse {

    /** Definition ID. */
    private String definitionId;

    /** Message code. */
    private String messageCode;

    /** Message name. */
    private String messageName;

    /** Business type ID. */
    private String businessTypeId;

    /** Message direction. */
    private MessageDirection direction;

    /** Field count. */
    private int fieldCount;

    /** Field summary. */
    private String fieldSummary;

    /** Sample XML. */
    private String sampleXml;

    /** Definition status. */
    private EnableDisableStatus definitionStatus;

    /** Sort order. */
    private int sortOrder;

    /** Created timestamp. */
    private LocalDateTime createTime;

    /** Updated timestamp. */
    private LocalDateTime updateTime;

    /**
     * Build a response DTO from an entity.
     *
     * @param entity the message definition entity
     * @return response DTO
     */
    public static DefinitionResponse from(final BizMessageDefinition entity) {
        DefinitionResponse resp = new DefinitionResponse();
        resp.definitionId = entity.getDefinitionId();
        resp.messageCode = entity.getMessageCode();
        resp.messageName = entity.getMessageName();
        resp.businessTypeId = entity.getBusinessTypeId();
        resp.direction = entity.getDirection();
        resp.fieldCount = entity.getFieldCount();
        resp.fieldSummary = entity.getFieldSummary();
        resp.sampleXml = entity.getSampleXml();
        resp.definitionStatus = entity.getDefinitionStatus();
        resp.sortOrder = entity.getSortOrder();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * Get definition ID.
     *
     * @return definition ID
     */
    public String getDefinitionId() {
        return definitionId;
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
     * Get message name.
     *
     * @return message name
     */
    public String getMessageName() {
        return messageName;
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
     * Get field summary.
     *
     * @return field summary (may be null)
     */
    public String getFieldSummary() {
        return fieldSummary;
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
}
