package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code pzrzStatusInfo} — certificate financing status record.
 *
 * <p>Fields follow the XSD {@code pzrzStatusInfo} complexType sequence:
 * pzNo, rzPhaseCode, BankNodeCode. All fields are required.</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzrzStatusInfo")
@XmlType(propOrder = {"pzNo", "rzPhaseCode", "bankNodeCode"})
public class PzrzStatusInfo extends CfxBody {

    @XmlElement(name = "pzNo", required = true)
    private String pzNo;

    @XmlElement(name = "rzPhaseCode", required = true)
    private String rzPhaseCode;

    @XmlElement(name = "BankNodeCode", required = true)
    private String bankNodeCode;

    /**
     * @return certificate number
     */
    public String getPzNo() {
        return pzNo;
    }

    /**
     * @param v certificate number
     */
    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    /**
     * @return financing phase code
     */
    public String getRzPhaseCode() {
        return rzPhaseCode;
    }

    /**
     * @param v financing phase code
     */
    public void setRzPhaseCode(final String v) {
        this.rzPhaseCode = v;
    }

    /**
     * @return accepting bank node code
     */
    public String getBankNodeCode() {
        return bankNodeCode;
    }

    /**
     * @param v accepting bank node code
     */
    public void setBankNodeCode(final String v) {
        this.bankNodeCode = v;
    }
}
