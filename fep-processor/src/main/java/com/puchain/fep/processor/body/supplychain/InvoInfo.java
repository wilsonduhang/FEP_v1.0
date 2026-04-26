package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3105 发票信息段 ({@code InvoInfo}, local complexType in 3105.xsd).
 *
 * <p>Field order matches the XSD sequence (15 fields, 6 required + 9 optional):
 * InvoSerial, ContractNo, InvoCode?, InvoNo, CheckCode?, InvoAmtTax, InvoAmt, InvoDate,
 * InvoAmtUsed?, InvoFilename?, InvoCAFilename?, xsfName?, kpfName?, ghfName?, spfName?。</p>
 *
 * <p>Used as repeating ({@code maxOccurs=10}) nested element of {@link RzApplyInfo3105}
 * — modeled there as {@code List&lt;InvoInfo&gt;}. All field types are {@link String};
 * {@code InvoSerial} is logically integer but XSD-validated, and converter-layer
 * format checks are owned by {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * <p>Getter/setter Javadoc 省略以与 {@link com.puchain.fep.processor.body.common.PzInfo}
 * 等高字段数 POJO 风格保持一致（避免触发 checkstyle FileLength 上限）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "InvoInfo")
@XmlType(propOrder = {
        "invoSerial", "contractNo", "invoCode", "invoNo", "checkCode",
        "invoAmtTax", "invoAmt", "invoDate", "invoAmtUsed",
        "invoFilename", "invoCAFilename",
        "xsfName", "kpfName", "ghfName", "spfName"
})
public class InvoInfo extends CfxBody {

    @XmlElement(name = "InvoSerial", required = true)
    private String invoSerial;

    @XmlElement(name = "ContractNo", required = true)
    private String contractNo;

    @XmlElement(name = "InvoCode")
    private String invoCode;

    @XmlElement(name = "InvoNo", required = true)
    private String invoNo;

    @XmlElement(name = "CheckCode")
    private String checkCode;

    @XmlElement(name = "InvoAmtTax", required = true)
    private String invoAmtTax;

    @XmlElement(name = "InvoAmt", required = true)
    private String invoAmt;

    @XmlElement(name = "InvoDate", required = true)
    private String invoDate;

    @XmlElement(name = "InvoAmtUsed")
    private String invoAmtUsed;

    @XmlElement(name = "InvoFilename")
    private String invoFilename;

    @XmlElement(name = "InvoCAFilename")
    private String invoCAFilename;

    @XmlElement(name = "xsfName")
    private String xsfName;

    @XmlElement(name = "kpfName")
    private String kpfName;

    @XmlElement(name = "ghfName")
    private String ghfName;

    @XmlElement(name = "spfName")
    private String spfName;

    public String getInvoSerial() {
        return invoSerial;
    }

    public void setInvoSerial(final String v) {
        this.invoSerial = v;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(final String v) {
        this.contractNo = v;
    }

    public String getInvoCode() {
        return invoCode;
    }

    public void setInvoCode(final String v) {
        this.invoCode = v;
    }

    public String getInvoNo() {
        return invoNo;
    }

    public void setInvoNo(final String v) {
        this.invoNo = v;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(final String v) {
        this.checkCode = v;
    }

    public String getInvoAmtTax() {
        return invoAmtTax;
    }

    public void setInvoAmtTax(final String v) {
        this.invoAmtTax = v;
    }

    public String getInvoAmt() {
        return invoAmt;
    }

    public void setInvoAmt(final String v) {
        this.invoAmt = v;
    }

    public String getInvoDate() {
        return invoDate;
    }

    public void setInvoDate(final String v) {
        this.invoDate = v;
    }

    public String getInvoAmtUsed() {
        return invoAmtUsed;
    }

    public void setInvoAmtUsed(final String v) {
        this.invoAmtUsed = v;
    }

    public String getInvoFilename() {
        return invoFilename;
    }

    public void setInvoFilename(final String v) {
        this.invoFilename = v;
    }

    public String getInvoCAFilename() {
        return invoCAFilename;
    }

    public void setInvoCAFilename(final String v) {
        this.invoCAFilename = v;
    }

    public String getXsfName() {
        return xsfName;
    }

    public void setXsfName(final String v) {
        this.xsfName = v;
    }

    public String getKpfName() {
        return kpfName;
    }

    public void setKpfName(final String v) {
        this.kpfName = v;
    }

    public String getGhfName() {
        return ghfName;
    }

    public void setGhfName(final String v) {
        this.ghfName = v;
    }

    public String getSpfName() {
        return spfName;
    }

    public void setSpfName(final String v) {
        this.spfName = v;
    }
}
