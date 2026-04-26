package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3105 合同信息段 ({@code ContractInfo}, local complexType in 3105.xsd).
 *
 * <p>Field order matches the XSD sequence (10 fields, 5 required + 5 optional):
 * ContractNo, ContractAmt, jfqyName, jfqyCode?, yfqyName, yfqyCode?, sxDate,
 * qzDate?, ContractFilename?, CertFilename?。</p>
 *
 * <p><b>与 {@link ContractInfo3101} 主类的关系</b>: {@link ContractInfo3101} 是 3101
 * 合同归档主报文 body POJO（23 字段，主类带 {@code @XmlType(name="ContractInfo3101")}），
 * 而本类是 3105 融资申请的支撑型 nested complexType（10 字段，本地 type，未带 name）。
 * 两者 {@code @XmlRootElement} 名称不同 ({@code ContractInfo} vs {@code ContractInfo3101})，
 * 在 JAXB 上下文中并存无冲突；命名上保持简短的 {@code ContractInfo} 与 XSD complexType
 * 同名以减少跨层翻译成本。</p>
 *
 * <p>Used as repeating ({@code maxOccurs=10}) nested element of {@link RzApplyInfo3105} —
 * modeled there as {@code List&lt;ContractInfo&gt;}. All field types are {@link String};
 * XSD constraints enforced by {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ContractInfo")
@XmlType(propOrder = {
        "contractNo", "contractAmt",
        "jfqyName", "jfqyCode", "yfqyName", "yfqyCode",
        "sxDate", "qzDate",
        "contractFilename", "certFilename"
})
public class ContractInfo extends CfxBody {

    @XmlElement(name = "ContractNo", required = true)
    private String contractNo;

    @XmlElement(name = "ContractAmt", required = true)
    private String contractAmt;

    @XmlElement(name = "jfqyName", required = true)
    private String jfqyName;

    @XmlElement(name = "jfqyCode")
    private String jfqyCode;

    @XmlElement(name = "yfqyName", required = true)
    private String yfqyName;

    @XmlElement(name = "yfqyCode")
    private String yfqyCode;

    @XmlElement(name = "sxDate", required = true)
    private String sxDate;

    @XmlElement(name = "qzDate")
    private String qzDate;

    @XmlElement(name = "ContractFilename")
    private String contractFilename;

    @XmlElement(name = "CertFilename")
    private String certFilename;

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(final String v) {
        this.contractNo = v;
    }

    public String getContractAmt() {
        return contractAmt;
    }

    public void setContractAmt(final String v) {
        this.contractAmt = v;
    }

    public String getJfqyName() {
        return jfqyName;
    }

    public void setJfqyName(final String v) {
        this.jfqyName = v;
    }

    public String getJfqyCode() {
        return jfqyCode;
    }

    public void setJfqyCode(final String v) {
        this.jfqyCode = v;
    }

    public String getYfqyName() {
        return yfqyName;
    }

    public void setYfqyName(final String v) {
        this.yfqyName = v;
    }

    public String getYfqyCode() {
        return yfqyCode;
    }

    public void setYfqyCode(final String v) {
        this.yfqyCode = v;
    }

    public String getSxDate() {
        return sxDate;
    }

    public void setSxDate(final String v) {
        this.sxDate = v;
    }

    public String getQzDate() {
        return qzDate;
    }

    public void setQzDate(final String v) {
        this.qzDate = v;
    }

    public String getContractFilename() {
        return contractFilename;
    }

    public void setContractFilename(final String v) {
        this.contractFilename = v;
    }

    public String getCertFilename() {
        return certFilename;
    }

    public void setCertFilename(final String v) {
        this.certFilename = v;
    }
}
