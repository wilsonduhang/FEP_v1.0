package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3105 融资资金信息段 ({@code rzAmtInfo}, local complexType in 3105.xsd).
 *
 * <p>Field order matches the XSD sequence (9 fields):
 * rzAmt, rzRate?, lxAmt?, BankSCAmt?, ApplyDate, EndDate, fxMode?, fkcnNo?, rzPurpose?。</p>
 *
 * <p><b>独立类原因</b>: XSD diff 实测 3009 {@code rzAmtInfo} (12 字段, 含 BillNo/BankSCRate/
 * TotalSCAmt/RepayMode/LoanAccInfo) 与 3105 {@code rzAmtInfo} (9 字段, 含 ApplyDate/EndDate/
 * fxMode/fkcnNo/rzPurpose) 字段集合完全不同，故拆为独立 POJO ({@link RzAmtInfo3009} vs
 * {@code RzAmtInfo3105})。命名带 {@code 3105} 后缀以避免歧义。</p>
 *
 * <p><b>JAXB binding 注意</b>: {@code RzAmtInfo3009} 与 {@code RzAmtInfo3105} 都映射到
 * {@code @XmlRootElement(name="rzAmtInfo")} 但绑定上下文相互隔离 — 各报文测试 / 业务流程
 * 仅使用本报文主类 ({@link RzApplyInfo3105}) 创建的 {@link jakarta.xml.bind.JAXBContext}，
 * binding 范围互不重叠。禁止把两类放入同一 context。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rzAmtInfo")
@XmlType(propOrder = {
        "rzAmt", "rzRate", "lxAmt", "bankSCAmt",
        "applyDate", "endDate", "fxMode", "fkcnNo", "rzPurpose"
})
public class RzAmtInfo3105 extends CfxBody {

    @XmlElement(name = "rzAmt", required = true)
    private String rzAmt;

    @XmlElement(name = "rzRate")
    private String rzRate;

    @XmlElement(name = "lxAmt")
    private String lxAmt;

    @XmlElement(name = "BankSCAmt")
    private String bankSCAmt;

    @XmlElement(name = "ApplyDate", required = true)
    private String applyDate;

    @XmlElement(name = "EndDate", required = true)
    private String endDate;

    @XmlElement(name = "fxMode")
    private String fxMode;

    @XmlElement(name = "fkcnNo")
    private String fkcnNo;

    @XmlElement(name = "rzPurpose")
    private String rzPurpose;

    public String getRzAmt() {
        return rzAmt;
    }

    public void setRzAmt(final String v) {
        this.rzAmt = v;
    }

    public String getRzRate() {
        return rzRate;
    }

    public void setRzRate(final String v) {
        this.rzRate = v;
    }

    public String getLxAmt() {
        return lxAmt;
    }

    public void setLxAmt(final String v) {
        this.lxAmt = v;
    }

    public String getBankSCAmt() {
        return bankSCAmt;
    }

    public void setBankSCAmt(final String v) {
        this.bankSCAmt = v;
    }

    public String getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(final String v) {
        this.applyDate = v;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String v) {
        this.endDate = v;
    }

    public String getFxMode() {
        return fxMode;
    }

    public void setFxMode(final String v) {
        this.fxMode = v;
    }

    public String getFkcnNo() {
        return fkcnNo;
    }

    public void setFkcnNo(final String v) {
        this.fkcnNo = v;
    }

    public String getRzPurpose() {
        return rzPurpose;
    }

    public void setRzPurpose(final String v) {
        this.rzPurpose = v;
    }
}
