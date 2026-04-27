package com.puchain.fep.web.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 触发当日 3116 银行资金日对账的请求 DTO（PRD §2137）。
 *
 * <p>P2e Task 7 — Web Controller 层入参，{@code messageType} 当前仅支持
 * {@code "3116"}（其他对账报文 3107/3108 通过专用端点驱动）。{@code date}
 * 必须为 {@code yyyyMMdd} 8 位数字串，对齐
 * {@link com.puchain.fep.processor.body.supplychain.BankCheckDay3116#getCheckDate()}
 * 的报文字段格式。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "触发当日对账请求", name = "DailyReconciliationRequest")
public class DailyReconciliationRequest {

    @Schema(description = "对账业务日期（yyyyMMdd）", example = "20260427", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "date 不能为空")
    @Pattern(regexp = "\\d{8}", message = "date 必须为 yyyyMMdd 8 位数字")
    private String date;

    @Schema(description = "报文类型，当前固定 3116", example = "3116", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "messageType 不能为空")
    private String messageType;

    /**
     * Default constructor for Jackson.
     */
    public DailyReconciliationRequest() {
        // Jackson
    }

    /**
     * Returns the business reconciliation date.
     *
     * @return reconciliation date in yyyyMMdd format
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the reconciliation date.
     *
     * @param v date in yyyyMMdd format
     */
    public void setDate(final String v) {
        this.date = v;
    }

    /**
     * Returns the HNDEMP message type code.
     *
     * @return message type, currently fixed to 3116
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the message type.
     *
     * @param v message type code
     */
    public void setMessageType(final String v) {
        this.messageType = v;
    }
}
