package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3101 合同信息报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3101.xsd} 中 {@code ContractInfo3101} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, ContractNo, hxqyCode?, ContractType, DigitalSeal,
 * ContractFilename, CertFilename?, jfqyName, jfqyCode?, yfqyName, yfqyCode?, qyName3?,
 * qyCode3?, qyName4?, qyCode4?, sxDate?, qzDate?, ywValue1?, ywValue2?, ContractReturnMemo?,
 * ExtInfo?。共 23 字段（9 必填 + 14 可选）。</p>
 *
 * <p>{@link ExtInfo} 为可选嵌套扩展块（{@code body.common.ExtInfo}）。所有文本字段
 * Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>Getter/setter Javadoc 省略以与 {@link com.puchain.fep.processor.body.common.PzInfo}
 * 等高字段数 POJO 风格保持一致（避免触发 checkstyle FileLength 上限）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ContractInfo3101")
@XmlType(name = "ContractInfo3101", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "contractNo", "hxqyCode", "contractType",
        "digitalSeal", "contractFilename", "certFilename",
        "jfqyName", "jfqyCode", "yfqyName", "yfqyCode",
        "qyName3", "qyCode3", "qyName4", "qyCode4",
        "sxDate", "qzDate", "ywValue1", "ywValue2",
        "contractReturnMemo", "extInfo"
})
public class ContractInfo3101 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "ContractNo", required = true)
    private String contractNo;

    @XmlElement(name = "hxqyCode")
    private String hxqyCode;

    @XmlElement(name = "ContractType", required = true)
    private String contractType;

    @XmlElement(name = "DigitalSeal", required = true)
    private String digitalSeal;

    @XmlElement(name = "ContractFilename", required = true)
    private String contractFilename;

    @XmlElement(name = "CertFilename")
    private String certFilename;

    @XmlElement(name = "jfqyName", required = true)
    private String jfqyName;

    @XmlElement(name = "jfqyCode")
    private String jfqyCode;

    @XmlElement(name = "yfqyName", required = true)
    private String yfqyName;

    @XmlElement(name = "yfqyCode")
    private String yfqyCode;

    @XmlElement(name = "qyName3")
    private String qyName3;

    @XmlElement(name = "qyCode3")
    private String qyCode3;

    @XmlElement(name = "qyName4")
    private String qyName4;

    @XmlElement(name = "qyCode4")
    private String qyCode4;

    @XmlElement(name = "sxDate")
    private String sxDate;

    @XmlElement(name = "qzDate")
    private String qzDate;

    @XmlElement(name = "ywValue1")
    private String ywValue1;

    @XmlElement(name = "ywValue2")
    private String ywValue2;

    @XmlElement(name = "ContractReturnMemo")
    private String contractReturnMemo;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    public String getSendNodeCode() {
        return sendNodeCode;
    }

    public void setSendNodeCode(final String v) {
        this.sendNodeCode = v;
    }

    public String getDesNodeCode() {
        return desNodeCode;
    }

    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(final String v) {
        this.contractNo = v;
    }

    public String getHxqyCode() {
        return hxqyCode;
    }

    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(final String v) {
        this.contractType = v;
    }

    public String getDigitalSeal() {
        return digitalSeal;
    }

    public void setDigitalSeal(final String v) {
        this.digitalSeal = v;
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

    public String getQyName3() {
        return qyName3;
    }

    public void setQyName3(final String v) {
        this.qyName3 = v;
    }

    public String getQyCode3() {
        return qyCode3;
    }

    public void setQyCode3(final String v) {
        this.qyCode3 = v;
    }

    public String getQyName4() {
        return qyName4;
    }

    public void setQyName4(final String v) {
        this.qyName4 = v;
    }

    public String getQyCode4() {
        return qyCode4;
    }

    public void setQyCode4(final String v) {
        this.qyCode4 = v;
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

    public String getYwValue1() {
        return ywValue1;
    }

    public void setYwValue1(final String v) {
        this.ywValue1 = v;
    }

    public String getYwValue2() {
        return ywValue2;
    }

    public void setYwValue2(final String v) {
        this.ywValue2 = v;
    }

    public String getContractReturnMemo() {
        return contractReturnMemo;
    }

    public void setContractReturnMemo(final String v) {
        this.contractReturnMemo = v;
    }

    public ExtInfo getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
