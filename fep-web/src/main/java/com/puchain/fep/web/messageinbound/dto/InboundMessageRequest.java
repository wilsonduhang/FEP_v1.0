package com.puchain.fep.web.messageinbound.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 入站报文 REST 请求 DTO（PRD §5.3.2.13）。
 *
 * <p>P3 Task 2 — 暴露 {@code POST /api/v1/messages/inbound} 的入参。
 * 生产环境入站走 TLQ 通道（见 {@code TlqInboundListener}，P3 Task 3），
 * dev/test 环境走 REST 端点便于联调与集成测试。</p>
 *
 * <p>三个字段均为必填：</p>
 * <ul>
 *   <li>{@code messageType} — 4 位数字 HNDEMP 报文类型（如 {@code 3116}），用 {@link Pattern} 严控。</li>
 *   <li>{@code transitionNo} — 8 位业务流水号，对齐 {@code RealHead.transitionNo} 末段。</li>
 *   <li>{@code xmlBase64} — Base64 编码后的 CFX UTF-8 XML payload，由 dispatcher 解码 + 委派 {@code SyncMessageProcessorService}。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "入站报文请求", name = "InboundMessageRequest")
public class InboundMessageRequest {

    @Schema(description = "4 位数字 HNDEMP 报文类型", example = "3116",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "messageType 不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "messageType 必须为 4 位数字")
    private String messageType;

    @Schema(description = "8 位业务流水号", example = "20260428",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "transitionNo 不能为空")
    @Size(min = 8, max = 8, message = "transitionNo 必须为 8 位")
    private String transitionNo;

    @Schema(description = "Base64 编码后的 CFX UTF-8 XML payload",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "xmlBase64 不能为空")
    private String xmlBase64;

    /**
     * Default constructor for Jackson.
     */
    public InboundMessageRequest() {
        // Jackson
    }

    /**
     * Returns the 4-digit HNDEMP message type code.
     *
     * @return messageType
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the 4-digit HNDEMP message type code.
     *
     * @param v messageType
     */
    public void setMessageType(final String v) {
        this.messageType = v;
    }

    /**
     * Returns the 8-character business transition number.
     *
     * @return transitionNo
     */
    public String getTransitionNo() {
        return transitionNo;
    }

    /**
     * Sets the 8-character business transition number.
     *
     * @param v transitionNo
     */
    public void setTransitionNo(final String v) {
        this.transitionNo = v;
    }

    /**
     * Returns the Base64-encoded UTF-8 XML payload.
     *
     * @return xmlBase64
     */
    public String getXmlBase64() {
        return xmlBase64;
    }

    /**
     * Sets the Base64-encoded UTF-8 XML payload.
     *
     * @param v xmlBase64
     */
    public void setXmlBase64(final String v) {
        this.xmlBase64 = v;
    }
}
