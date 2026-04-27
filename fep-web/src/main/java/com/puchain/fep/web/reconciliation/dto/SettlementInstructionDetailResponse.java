package com.puchain.fep.web.reconciliation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 单条清算指令详情响应 DTO（PRD §2138 + §1995）。
 *
 * <p>12 字段映射至
 * {@link com.puchain.fep.processor.reconciliation.ClearingInstructionRecord}。
 * {@code settlementAmount} 使用 {@link ToStringSerializer} 防 JS 精度丢失。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "清算指令详情", name = "SettlementInstructionDetailResponse")
public class SettlementInstructionDetailResponse {

    @Schema(description = "指令 ID（即 PlatPayNo）")
    private String instructionId;

    @Schema(description = "清算流水号")
    private String qsSerialNo;

    @Schema(description = "指令类型", example = "NORMAL",
            allowableValues = {"NORMAL", "ERROR_HANDLING", "BUSINESS_CANCEL"})
    private String instructionType;

    @Schema(description = "结算金额")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal settlementAmount;

    @Schema(description = "付款方账号")
    private String payerAccount;

    @Schema(description = "收款方账号")
    private String payeeAccount;

    @Schema(description = "指令状态", example = "PENDING",
            allowableValues = {"PENDING", "PROCESSING", "SUCCESS", "FAILED"})
    private String instructionStatus;

    @Schema(description = "执行时间（SUCCESS/FAILED 才有值）")
    private LocalDateTime executionTime;

    @Schema(description = "失败原因（仅 FAILED 状态）")
    private String failureCause;

    @Schema(description = "关联报文记录 ID")
    private String messageId;

    @Schema(description = "记录创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "记录更新时间")
    private LocalDateTime updatedAt;

    /**
     * Default constructor for Jackson.
     */
    public SettlementInstructionDetailResponse() {
        // Jackson
    }

    /**
     * Maps a domain record to a response DTO.
     *
     * @param r source record, non-null
     * @return mapped DTO
     */
    public static SettlementInstructionDetailResponse from(final ClearingInstructionRecord r) {
        Objects.requireNonNull(r, "r");
        final SettlementInstructionDetailResponse dto = new SettlementInstructionDetailResponse();
        dto.instructionId = r.getInstructionId();
        dto.qsSerialNo = r.getQsSerialNo();
        dto.instructionType = r.getInstructionType();
        dto.settlementAmount = r.getSettlementAmount();
        dto.payerAccount = r.getPayerAccount();
        dto.payeeAccount = r.getPayeeAccount();
        dto.instructionStatus = r.getInstructionStatus();
        dto.executionTime = r.getExecutionTime();
        dto.failureCause = r.getFailureCause();
        dto.messageId = r.getMessageId();
        dto.createdAt = r.getCreatedAt();
        dto.updatedAt = r.getUpdatedAt();
        return dto;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(final String v) {
        this.instructionId = v;
    }

    public String getQsSerialNo() {
        return qsSerialNo;
    }

    public void setQsSerialNo(final String v) {
        this.qsSerialNo = v;
    }

    public String getInstructionType() {
        return instructionType;
    }

    public void setInstructionType(final String v) {
        this.instructionType = v;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(final BigDecimal v) {
        this.settlementAmount = v;
    }

    public String getPayerAccount() {
        return payerAccount;
    }

    public void setPayerAccount(final String v) {
        this.payerAccount = v;
    }

    public String getPayeeAccount() {
        return payeeAccount;
    }

    public void setPayeeAccount(final String v) {
        this.payeeAccount = v;
    }

    public String getInstructionStatus() {
        return instructionStatus;
    }

    public void setInstructionStatus(final String v) {
        this.instructionStatus = v;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(final LocalDateTime v) {
        this.executionTime = v;
    }

    public String getFailureCause() {
        return failureCause;
    }

    public void setFailureCause(final String v) {
        this.failureCause = v;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String v) {
        this.messageId = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final LocalDateTime v) {
        this.createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final LocalDateTime v) {
        this.updatedAt = v;
    }
}
