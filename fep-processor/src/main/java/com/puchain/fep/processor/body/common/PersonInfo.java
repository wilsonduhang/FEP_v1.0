package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code PersonInfo} (DataType.xsd) — person identity block.
 *
 * <p>Fields follow the XSD {@code PersonInfo} complexType sequence:
 * Name (required), CertType, CertNumber, CertStartDate, CertEndDate,
 * Phone (required), PostAddr, MailAddr (8 fields total).</p>
 *
 * <p>Used as a nested person-identity block by supply-chain messages such as
 * 3102 / 3109 (legal representative / authorized signatory).</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.
 * Java field {@code name} maps to XSD element {@code Name} via {@link XmlElement#name()}
 * to avoid Java reserved-word collision (precedent: {@code ExtInfo.extJsonFilename}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PersonInfo")
@XmlType(propOrder = {
        "name", "certType", "certNumber", "certStartDate", "certEndDate",
        "phone", "postAddr", "mailAddr"
})
public class PersonInfo extends CfxBody {

    @XmlElement(name = "Name", required = true)
    private String name;

    @XmlElement(name = "CertType")
    private String certType;

    @XmlElement(name = "CertNumber")
    private String certNumber;

    @XmlElement(name = "CertStartDate")
    private String certStartDate;

    @XmlElement(name = "CertEndDate")
    private String certEndDate;

    @XmlElement(name = "Phone", required = true)
    private String phone;

    @XmlElement(name = "PostAddr")
    private String postAddr;

    @XmlElement(name = "MailAddr")
    private String mailAddr;

    public String getName() {
        return name;
    }

    public void setName(final String v) {
        this.name = v;
    }

    public String getCertType() {
        return certType;
    }

    public void setCertType(final String v) {
        this.certType = v;
    }

    public String getCertNumber() {
        return certNumber;
    }

    public void setCertNumber(final String v) {
        this.certNumber = v;
    }

    public String getCertStartDate() {
        return certStartDate;
    }

    public void setCertStartDate(final String v) {
        this.certStartDate = v;
    }

    public String getCertEndDate() {
        return certEndDate;
    }

    public void setCertEndDate(final String v) {
        this.certEndDate = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String v) {
        this.phone = v;
    }

    public String getPostAddr() {
        return postAddr;
    }

    public void setPostAddr(final String v) {
        this.postAddr = v;
    }

    public String getMailAddr() {
        return mailAddr;
    }

    public void setMailAddr(final String v) {
        this.mailAddr = v;
    }
}
