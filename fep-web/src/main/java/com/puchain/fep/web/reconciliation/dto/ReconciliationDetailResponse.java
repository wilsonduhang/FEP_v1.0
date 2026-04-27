package com.puchain.fep.web.reconciliation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 单条对账记录详情响应 DTO（PRD §2137 + §1983）。
 *
 * <p>13 字段映射至
 * {@link com.puchain.fep.processor.reconciliation.ReconciliationRecord}。
 * {@code totalTransactionAmount} 使用 {@link ToStringSerializer} 防 JS
 * 精度丢失，与 P6d {@code SubmissionRecordResponse.amount} 保持一致风格。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "对账记录详情", name = "ReconciliationDetailResponse")
public class ReconciliationDetailResponse {

    @Schema(description = "对账编号", example = "RC_20260427_001")
    private String reconciliationId;

    @Schema(description = "对账日期", example = "2026-04-27")
    private LocalDate reconciliationDate;

    @Schema(description = "报文类型", example = "3116")
    private String messageType;

    @Schema(description = "业务流水号", example = "SN20260427000001")
    private String serialNo;

    @Schema(description = "配对流水号（3107/3108 双向追溯）")
    private String pairedSerialNo;

    @Schema(description = "声明的交易笔数")
    private int totalTransactionCount;

    @Schema(description = "声明的交易总金额")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalTransactionAmount;

    @Schema(description = "实际观察到的明细条数")
    private int actualCount;

    @Schema(description = "对账状态", example = "COMPLETED",
            allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "DISCREPANCY"})
    private String status;

    @Schema(description = "差异条目数")
    private int discrepancyCount;

    @Schema(description = "对账执行时间")
    private LocalDateTime reconciliationTime;

    @Schema(description = "记录创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "记录更新时间")
    private LocalDateTime updatedAt;

    /**
     * Default constructor for Jackson.
     */
    public ReconciliationDetailResponse() {
        // Jackson
    }

    /**
     * Maps an immutable {@link ReconciliationRecord} to a response DTO.
     *
     * @param r source record, non-null
     * @return mapped DTO
     */
    public static ReconciliationDetailResponse from(final ReconciliationRecord r) {
        Objects.requireNonNull(r, "r");
        final ReconciliationDetailResponse dto = new ReconciliationDetailResponse();
        dto.reconciliationId = r.getReconciliationId();
        dto.reconciliationDate = r.getReconciliationDate();
        dto.messageType = r.getMessageType();
        dto.serialNo = r.getSerialNo();
        dto.pairedSerialNo = r.getPairedSerialNo();
        dto.totalTransactionCount = r.getTotalTransactionCount();
        dto.totalTransactionAmount = r.getTotalTransactionAmount();
        dto.actualCount = r.getActualCount();
        dto.status = r.getStatus();
        dto.discrepancyCount = r.getDiscrepancyCount();
        dto.reconciliationTime = r.getReconciliationTime();
        dto.createdAt = r.getCreatedAt();
        dto.updatedAt = r.getUpdatedAt();
        return dto;
    }

    public String getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(final String v) {
        this.reconciliationId = v;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(final LocalDate v) {
        this.reconciliationDate = v;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(final String v) {
        this.messageType = v;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    public String getPairedSerialNo() {
        return pairedSerialNo;
    }

    public void setPairedSerialNo(final String v) {
        this.pairedSerialNo = v;
    }

    public int getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public void setTotalTransactionCount(final int v) {
        this.totalTransactionCount = v;
    }

    public BigDecimal getTotalTransactionAmount() {
        return totalTransactionAmount;
    }

    public void setTotalTransactionAmount(final BigDecimal v) {
        this.totalTransactionAmount = v;
    }

    public int getActualCount() {
        return actualCount;
    }

    public void setActualCount(final int v) {
        this.actualCount = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String v) {
        this.status = v;
    }

    public int getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(final int v) {
        this.discrepancyCount = v;
    }

    public LocalDateTime getReconciliationTime() {
        return reconciliationTime;
    }

    public void setReconciliationTime(final LocalDateTime v) {
        this.reconciliationTime = v;
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
