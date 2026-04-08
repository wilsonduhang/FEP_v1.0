package com.puchain.fep.web.bizdata.record.dto;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a business message record (manual entry).
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RecordCreateRequest {

    /** Message code (1-5 digit number). */
    @NotBlank(message = "报文编码不能为空")
    @Pattern(regexp = "\\d{1,5}", message = "报文编码为 1-5 位数字")
    private String messageCode;

    /** Serial number (unique). */
    @NotBlank(message = "流水号不能为空")
    @Size(max = 50, message = "流水号长度不能超过 50 字符")
    private String serialNo;

    /** Message direction. */
    @NotNull(message = "报文方向不能为空")
    private MessageDirection direction;

    /** Sender node code (optional). */
    @Size(max = 20, message = "发送节点长度不能超过 20 字符")
    private String senderNode;

    /** Receiver node code (optional). */
    @Size(max = 20, message = "接收节点长度不能超过 20 字符")
    private String receiverNode;

    /** Business number (optional). */
    @Size(max = 100, message = "业务编号长度不能超过 100 字符")
    private String businessNo;

    /** Transaction amount (optional). */
    private BigDecimal amount;

    /** XML content (optional). */
    private String xmlContent;

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
     * Get serial number.
     *
     * @return serial number
     */
    public String getSerialNo() {
        return serialNo;
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
     * Get sender node.
     *
     * @return sender node (may be null)
     */
    public String getSenderNode() {
        return senderNode;
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
     * Get receiver node.
     *
     * @return receiver node (may be null)
     */
    public String getReceiverNode() {
        return receiverNode;
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
     * Get business number.
     *
     * @return business number (may be null)
     */
    public String getBusinessNo() {
        return businessNo;
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
     * Get amount.
     *
     * @return amount (may be null)
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Set amount.
     *
     * @param amount amount
     */
    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
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
     * Set XML content.
     *
     * @param xmlContent XML content
     */
    public void setXmlContent(final String xmlContent) {
        this.xmlContent = xmlContent;
    }
}
