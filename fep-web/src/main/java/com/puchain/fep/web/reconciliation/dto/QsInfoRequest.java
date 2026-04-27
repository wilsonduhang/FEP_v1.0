package com.puchain.fep.web.reconciliation.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 单条清算指令明细的请求 DTO（PRD §2138）。嵌套于
 * {@link SettlementInstructionRequest#getQsInfo()}，对应
 * {@link com.puchain.fep.processor.body.supplychain.QsInfo} 业务体。
 *
 * <p>有意省略 PK7 签名字段（{@code SignElement} / {@code qsfqSign} /
 * {@code PlatSign}）— Mode E 安全集成尚未到位，由
 * {@link com.puchain.fep.processor.reconciliation.ClearingInstructionService}
 * 在 service 层校验它们必须为 null。</p>
 *
 * <p>{@link BigDecimal} 金额字段使用 {@link ToStringSerializer} 防 JS 精度丢失，
 * 与 P6d {@code SubmissionRecordResponse.amount} 保持一致风格。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "清算指令明细", name = "QsInfoRequest")
public class QsInfoRequest {

    @Schema(description = "清算指令流水号", example = "QS20260427000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "qsSerialNo 不能为空")
    private String qsSerialNo;

    @Schema(description = "结算金额（元，必须为正数；正负性由 service 层 validateBusinessRule 判定）",
            example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "amt 不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amt;

    @Schema(description = "付款方账号", example = "6228480000000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "fkfAccNo 不能为空")
    private String fkfAccNo;

    @Schema(description = "收款方账号", example = "6228480000000002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "skfAccNo 不能为空")
    private String skfAccNo;

    @Schema(description = "付款方户名", example = "深圳供应链有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "fkfAccName 不能为空")
    private String fkfAccName;

    @Schema(description = "收款方户名", example = "上海融资保理有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "skfAccName 不能为空")
    private String skfAccName;

    @Schema(description = "期望执行日（yyyyMMdd）", example = "20260427", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "wishDate 不能为空")
    private String wishDate;

    /**
     * Default constructor for Jackson.
     */
    public QsInfoRequest() {
        // Jackson
    }

    /**
     * Returns the clearing serial number.
     *
     * @return qs serial number
     */
    public String getQsSerialNo() {
        return qsSerialNo;
    }

    /**
     * Sets the clearing serial number.
     *
     * @param v qs serial number
     */
    public void setQsSerialNo(final String v) {
        this.qsSerialNo = v;
    }

    /**
     * Returns the settlement amount.
     *
     * @return amount
     */
    public BigDecimal getAmt() {
        return amt;
    }

    /**
     * Sets the settlement amount.
     *
     * @param v amount, positive
     */
    public void setAmt(final BigDecimal v) {
        this.amt = v;
    }

    /**
     * Returns the payer account number.
     *
     * @return payer account
     */
    public String getFkfAccNo() {
        return fkfAccNo;
    }

    /**
     * Sets the payer account number.
     *
     * @param v payer account
     */
    public void setFkfAccNo(final String v) {
        this.fkfAccNo = v;
    }

    /**
     * Returns the payee account number.
     *
     * @return payee account
     */
    public String getSkfAccNo() {
        return skfAccNo;
    }

    /**
     * Sets the payee account number.
     *
     * @param v payee account
     */
    public void setSkfAccNo(final String v) {
        this.skfAccNo = v;
    }

    /**
     * Returns the payer account name.
     *
     * @return payer name
     */
    public String getFkfAccName() {
        return fkfAccName;
    }

    /**
     * Sets the payer account name.
     *
     * @param v payer name
     */
    public void setFkfAccName(final String v) {
        this.fkfAccName = v;
    }

    /**
     * Returns the payee account name.
     *
     * @return payee name
     */
    public String getSkfAccName() {
        return skfAccName;
    }

    /**
     * Sets the payee account name.
     *
     * @param v payee name
     */
    public void setSkfAccName(final String v) {
        this.skfAccName = v;
    }

    /**
     * Returns the desired execution date (yyyyMMdd).
     *
     * @return wish date
     */
    public String getWishDate() {
        return wishDate;
    }

    /**
     * Sets the desired execution date.
     *
     * @param v wish date in yyyyMMdd format
     */
    public void setWishDate(final String v) {
        this.wishDate = v;
    }
}
