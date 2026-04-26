package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code qyAccInfo} (DataType.xsd) — enterprise account block.
 *
 * <p>Fields follow the XSD {@code qyAccInfo} complexType sequence:
 * AccName, AccNumber, AccBankName, AccBankCode (4 fields, all required by XSD).</p>
 *
 * <p>Used as a nested account information block by multiple supply-chain messages
 * (e.g. 3009 LoanAccInfo / 3102 rzqyAccInfo / 3105 rzqyAccInfo + RepayAccInfo).</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qyAccInfo")
@XmlType(propOrder = {"accName", "accNumber", "accBankName", "accBankCode"})
public class QyAccInfo extends CfxBody {

    @XmlElement(name = "AccName", required = true)
    private String accName;

    @XmlElement(name = "AccNumber", required = true)
    private String accNumber;

    @XmlElement(name = "AccBankName", required = true)
    private String accBankName;

    @XmlElement(name = "AccBankCode", required = true)
    private String accBankCode;

    public String getAccName() {
        return accName;
    }

    public void setAccName(final String v) {
        this.accName = v;
    }

    public String getAccNumber() {
        return accNumber;
    }

    public void setAccNumber(final String v) {
        this.accNumber = v;
    }

    public String getAccBankName() {
        return accBankName;
    }

    public void setAccBankName(final String v) {
        this.accBankName = v;
    }

    public String getAccBankCode() {
        return accBankCode;
    }

    public void setAccBankCode(final String v) {
        this.accBankCode = v;
    }
}
