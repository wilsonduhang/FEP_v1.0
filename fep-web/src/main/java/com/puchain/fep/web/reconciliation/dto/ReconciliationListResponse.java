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
 * 对账记录列表项响应 DTO（精简字段，PRD §2137）。
 *
 * <p>用于 GET /reconciliation 分页列表，仅承载列表展示所需的核心字段，
 * 完整字段使用 {@link ReconciliationDetailResponse}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "对账记录列表项", name = "ReconciliationListResponse")
public class ReconciliationListResponse {

    @Schema(description = "对账编号", example = "RC_20260427_001")
    private String reconciliationId;

    @Schema(description = "对账日期", example = "2026-04-27")
    private LocalDate reconciliationDate;

    @Schema(description = "报文类型", example = "3116")
    private String messageType;

    @Schema(description = "业务流水号", example = "SN20260427000001")
    private String serialNo;

    @Schema(description = "声明交易笔数")
    private int totalTransactionCount;

    @Schema(description = "声明交易总金额")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalTransactionAmount;

    @Schema(description = "对账状态", example = "COMPLETED")
    private String status;

    @Schema(description = "差异条目数")
    private int discrepancyCount;

    @Schema(description = "对账执行时间")
    private LocalDateTime reconciliationTime;

    /**
     * Default constructor for Jackson.
     */
    public ReconciliationListResponse() {
        // Jackson
    }

    /**
     * Maps a domain record to a list response.
     *
     * @param r source record, non-null
     * @return mapped DTO
     */
    public static ReconciliationListResponse from(final ReconciliationRecord r) {
        Objects.requireNonNull(r, "r");
        final ReconciliationListResponse dto = new ReconciliationListResponse();
        dto.reconciliationId = r.getReconciliationId();
        dto.reconciliationDate = r.getReconciliationDate();
        dto.messageType = r.getMessageType();
        dto.serialNo = r.getSerialNo();
        dto.totalTransactionCount = r.getTotalTransactionCount();
        dto.totalTransactionAmount = r.getTotalTransactionAmount();
        dto.status = r.getStatus();
        dto.discrepancyCount = r.getDiscrepancyCount();
        dto.reconciliationTime = r.getReconciliationTime();
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
}
