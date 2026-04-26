package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.QyAccInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3009 融资结果信息段 ({@code rzAmtInfo}, local complexType in 3009.xsd).
 *
 * <p>Field order matches the XSD sequence (12 fields):
 * BillNo, rzAmt, rzStartDate, rzEndDate, lxAmt, rzRate, BankSCAmt, BankSCRate,
 * TotalSCAmt, rzNetAmt, RepayMode?, LoanAccInfo?。</p>
 *
 * <p><b>独立类原因</b>: XSD diff 实测 3009 {@code rzAmtInfo} 与 3105 {@code rzAmtInfo}
 * 字段集合完全不同 — 3009 含 12 字段（带 BankSCAmt/BankSCRate/TotalSCAmt/LoanAccInfo），
 * 3105 仅 9 字段。两者必须拆为独立 POJO（{@code RzAmtInfo3009} vs {@code RzAmtInfo3105}）。
 * 命名带 {@code 3009} 后缀以避免歧义。</p>
 *
 * <p>{@code LoanAccInfo} 字段类型为 {@link QyAccInfo}（DataType.xsd 共享 complexType
 * {@code qyAccInfo}，T0.5 已落地）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rzAmtInfo")
@XmlType(propOrder = {
        "billNo", "rzAmt", "rzStartDate", "rzEndDate", "lxAmt",
        "rzRate", "bankSCAmt", "bankSCRate", "totalSCAmt", "rzNetAmt",
        "repayMode", "loanAccInfo"
})
public class RzAmtInfo3009 extends CfxBody {

    @XmlElement(name = "BillNo", required = true)
    private String billNo;

    @XmlElement(name = "rzAmt", required = true)
    private String rzAmt;

    @XmlElement(name = "rzStartDate", required = true)
    private String rzStartDate;

    @XmlElement(name = "rzEndDate", required = true)
    private String rzEndDate;

    @XmlElement(name = "lxAmt", required = true)
    private String lxAmt;

    @XmlElement(name = "rzRate", required = true)
    private String rzRate;

    @XmlElement(name = "BankSCAmt", required = true)
    private String bankSCAmt;

    @XmlElement(name = "BankSCRate", required = true)
    private String bankSCRate;

    @XmlElement(name = "TotalSCAmt", required = true)
    private String totalSCAmt;

    @XmlElement(name = "rzNetAmt", required = true)
    private String rzNetAmt;

    @XmlElement(name = "RepayMode")
    private String repayMode;

    @XmlElement(name = "LoanAccInfo")
    private QyAccInfo loanAccInfo;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(final String v) {
        this.billNo = v;
    }

    public String getRzAmt() {
        return rzAmt;
    }

    public void setRzAmt(final String v) {
        this.rzAmt = v;
    }

    public String getRzStartDate() {
        return rzStartDate;
    }

    public void setRzStartDate(final String v) {
        this.rzStartDate = v;
    }

    public String getRzEndDate() {
        return rzEndDate;
    }

    public void setRzEndDate(final String v) {
        this.rzEndDate = v;
    }

    public String getLxAmt() {
        return lxAmt;
    }

    public void setLxAmt(final String v) {
        this.lxAmt = v;
    }

    public String getRzRate() {
        return rzRate;
    }

    public void setRzRate(final String v) {
        this.rzRate = v;
    }

    public String getBankSCAmt() {
        return bankSCAmt;
    }

    public void setBankSCAmt(final String v) {
        this.bankSCAmt = v;
    }

    public String getBankSCRate() {
        return bankSCRate;
    }

    public void setBankSCRate(final String v) {
        this.bankSCRate = v;
    }

    public String getTotalSCAmt() {
        return totalSCAmt;
    }

    public void setTotalSCAmt(final String v) {
        this.totalSCAmt = v;
    }

    public String getRzNetAmt() {
        return rzNetAmt;
    }

    public void setRzNetAmt(final String v) {
        this.rzNetAmt = v;
    }

    public String getRepayMode() {
        return repayMode;
    }

    public void setRepayMode(final String v) {
        this.repayMode = v;
    }

    public QyAccInfo getLoanAccInfo() {
        return loanAccInfo;
    }

    public void setLoanAccInfo(final QyAccInfo v) {
        this.loanAccInfo = v;
    }
}
