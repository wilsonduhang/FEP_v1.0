package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3105 代收手续费信息段 ({@code ServiceChargeInfo}, local complexType in 3105.xsd).
 *
 * <p>Field order matches the XSD sequence (8 fields):
 * SCAccNo, SCAccName, SCAccBankName, SCAccBankCode, SCRate, SCAmtMin?, SCAmt, SCMemo。</p>
 *
 * <p>Used as nested element of {@link RzApplyInfo3105}; only relevant within 3105
 * 融资申请 message context. All field types are {@link String}; XSD constraints
 * are enforced by {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ServiceChargeInfo")
@XmlType(propOrder = {
        "scAccNo", "scAccName", "scAccBankName", "scAccBankCode",
        "scRate", "scAmtMin", "scAmt", "scMemo"
})
public class ServiceChargeInfo extends CfxBody {

    @XmlElement(name = "SCAccNo", required = true)
    private String scAccNo;

    @XmlElement(name = "SCAccName", required = true)
    private String scAccName;

    @XmlElement(name = "SCAccBankName", required = true)
    private String scAccBankName;

    @XmlElement(name = "SCAccBankCode", required = true)
    private String scAccBankCode;

    @XmlElement(name = "SCRate", required = true)
    private String scRate;

    @XmlElement(name = "SCAmtMin")
    private String scAmtMin;

    @XmlElement(name = "SCAmt", required = true)
    private String scAmt;

    @XmlElement(name = "SCMemo", required = true)
    private String scMemo;

    public String getScAccNo() {
        return scAccNo;
    }

    public void setScAccNo(final String v) {
        this.scAccNo = v;
    }

    public String getScAccName() {
        return scAccName;
    }

    public void setScAccName(final String v) {
        this.scAccName = v;
    }

    public String getScAccBankName() {
        return scAccBankName;
    }

    public void setScAccBankName(final String v) {
        this.scAccBankName = v;
    }

    public String getScAccBankCode() {
        return scAccBankCode;
    }

    public void setScAccBankCode(final String v) {
        this.scAccBankCode = v;
    }

    public String getScRate() {
        return scRate;
    }

    public void setScRate(final String v) {
        this.scRate = v;
    }

    public String getScAmtMin() {
        return scAmtMin;
    }

    public void setScAmtMin(final String v) {
        this.scAmtMin = v;
    }

    public String getScAmt() {
        return scAmt;
    }

    public void setScAmt(final String v) {
        this.scAmt = v;
    }

    public String getScMemo() {
        return scMemo;
    }

    public void setScMemo(final String v) {
        this.scMemo = v;
    }
}
