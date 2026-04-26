package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3009 融资担保信息段 ({@code dbInfo}, local complexType in 3009.xsd).
 *
 * <p>Field order matches the XSD sequence (6 fields):
 * dbAmt, dbFee, dbRate, dbqyName, dbNodeCode?, dbContractNo?。
 * 前 4 字段必填，后 2 字段可选。</p>
 *
 * <p>当前仅在 {@link RzReturnInfo3009} 中作为可选 {@code dbInfo} 字段引用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dbInfo")
@XmlType(propOrder = {
        "dbAmt", "dbFee", "dbRate", "dbqyName", "dbNodeCode", "dbContractNo"
})
public class DbInfo extends CfxBody {

    @XmlElement(name = "dbAmt", required = true)
    private String dbAmt;

    @XmlElement(name = "dbFee", required = true)
    private String dbFee;

    @XmlElement(name = "dbRate", required = true)
    private String dbRate;

    @XmlElement(name = "dbqyName", required = true)
    private String dbqyName;

    @XmlElement(name = "dbNodeCode")
    private String dbNodeCode;

    @XmlElement(name = "dbContractNo")
    private String dbContractNo;

    public String getDbAmt() {
        return dbAmt;
    }

    public void setDbAmt(final String v) {
        this.dbAmt = v;
    }

    public String getDbFee() {
        return dbFee;
    }

    public void setDbFee(final String v) {
        this.dbFee = v;
    }

    public String getDbRate() {
        return dbRate;
    }

    public void setDbRate(final String v) {
        this.dbRate = v;
    }

    public String getDbqyName() {
        return dbqyName;
    }

    public void setDbqyName(final String v) {
        this.dbqyName = v;
    }

    public String getDbNodeCode() {
        return dbNodeCode;
    }

    public void setDbNodeCode(final String v) {
        this.dbNodeCode = v;
    }

    public String getDbContractNo() {
        return dbContractNo;
    }

    public void setDbContractNo(final String v) {
        this.dbContractNo = v;
    }
}
