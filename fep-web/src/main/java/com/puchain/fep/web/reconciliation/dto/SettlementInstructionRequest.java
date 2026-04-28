package com.puchain.fep.web.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 发起 3115 资金清算指令的请求 DTO（PRD §2138 + §1995）。
 *
 * <p>P2e Task 7 — Web Controller 层入参。{@code platPayNo} 即为
 * {@link com.puchain.fep.processor.reconciliation.ClearingInstructionRecord#getInstructionId()}。
 * {@code qsInfo} 列表对齐 XSD 限定 1..200 条；超界由 {@link Size} 注解拦截。</p>
 *
 * <p>P3 Task 4 — 暴露 PK7 签名字段 ({@code signElement} / {@code qsfqSign} /
 * {@code platSign})，使 service 层守护通过 Controller REST 路径可达（关闭
 * ADR-P2e-4 Phase 1 偏离 #3）。Bean Validation 不加 {@code @AssertNull}：
 * 设计选择保留 service 层 last-line-of-defense 守护，理由是恶意/误用 PK7
 * 提交可被 service 层 audit 日志记录（DTO 校验仅返通用 400），Mode E 安全
 * 集成到位前不优化此 fast path（{@link com.puchain.fep.common.domain.FepErrorCode#CLEAR_BUSINESS_RULE_VIOLATION}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "3115 资金清算指令发起请求", name = "SettlementInstructionRequest")
public class SettlementInstructionRequest {

    @Schema(description = "清算指令平台编号", example = "PP20260427000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "platPayNo 不能为空")
    private String platPayNo;

    @Schema(description = "发送方节点代码（14 位）", example = "A1000143000104", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "sendNodeCode 不能为空")
    private String sendNodeCode;

    @Schema(description = "接收方节点代码（14 位）", example = "B2000456000204", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "desNodeCode 不能为空")
    private String desNodeCode;

    @Schema(description = "业务流水号", example = "SN20260427000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "serialNo 不能为空")
    private String serialNo;

    @Schema(description = "清算指令明细列表（1-200 条）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "qsInfo 不能为空")
    @Size(min = 1, max = 200, message = "qsInfo 必须为 1-200 条")
    @Valid
    private List<QsInfoRequest> qsInfo;

    @Schema(description = "PK7 签名元素（Mode E 集成前必须为 null/空字符串）", nullable = true)
    private String signElement;

    @Schema(description = "PK7 签发签名（Mode E 集成前必须为 null/空字符串）", nullable = true)
    private String qsfqSign;

    @Schema(description = "PK7 平台签名（Mode E 集成前必须为 null/空字符串）", nullable = true)
    private String platSign;

    /**
     * Default constructor for Jackson.
     */
    public SettlementInstructionRequest() {
        // Jackson
    }

    /**
     * Returns the platform settlement instruction number.
     *
     * @return platPayNo
     */
    public String getPlatPayNo() {
        return platPayNo;
    }

    /**
     * Sets the platform settlement instruction number.
     *
     * @param v platPayNo
     */
    public void setPlatPayNo(final String v) {
        this.platPayNo = v;
    }

    /**
     * Returns the sending node code.
     *
     * @return sendNodeCode
     */
    public String getSendNodeCode() {
        return sendNodeCode;
    }

    /**
     * Sets the sending node code.
     *
     * @param v sendNodeCode
     */
    public void setSendNodeCode(final String v) {
        this.sendNodeCode = v;
    }

    /**
     * Returns the destination node code.
     *
     * @return desNodeCode
     */
    public String getDesNodeCode() {
        return desNodeCode;
    }

    /**
     * Sets the destination node code.
     *
     * @param v desNodeCode
     */
    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    /**
     * Returns the business serial number.
     *
     * @return serialNo
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the business serial number.
     *
     * @param v serialNo
     */
    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    /**
     * Returns the clearing instruction list.
     *
     * @return qsInfo list
     */
    public List<QsInfoRequest> getQsInfo() {
        return qsInfo;
    }

    /**
     * Sets the clearing instruction list.
     *
     * @param v qsInfo list
     */
    public void setQsInfo(final List<QsInfoRequest> v) {
        this.qsInfo = v;
    }

    /**
     * Returns the PK7 SignElement value (Mode E placeholder; service-only guard).
     *
     * @return signElement
     */
    public String getSignElement() {
        return signElement;
    }

    /**
     * Sets the PK7 SignElement value.
     *
     * @param v signElement
     */
    public void setSignElement(final String v) {
        this.signElement = v;
    }

    /**
     * Returns the PK7 issuer signature value (Mode E placeholder; service-only guard).
     *
     * @return qsfqSign
     */
    public String getQsfqSign() {
        return qsfqSign;
    }

    /**
     * Sets the PK7 issuer signature value.
     *
     * @param v qsfqSign
     */
    public void setQsfqSign(final String v) {
        this.qsfqSign = v;
    }

    /**
     * Returns the PK7 platform signature value (Mode E placeholder; service-only guard).
     *
     * @return platSign
     */
    public String getPlatSign() {
        return platSign;
    }

    /**
     * Sets the PK7 platform signature value.
     *
     * @param v platSign
     */
    public void setPlatSign(final String v) {
        this.platSign = v;
    }
}
