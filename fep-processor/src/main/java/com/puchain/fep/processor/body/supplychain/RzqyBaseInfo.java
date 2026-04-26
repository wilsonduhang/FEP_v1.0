package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3102 局部 complexType {@code rzqyBaseInfo} — 融资企业基本信息段。
 *
 * <p>字段顺序严格对应 {@code 3102.xsd} 中 {@code rzqyBaseInfo} complexType 的 sequence：
 * qyCreditCode, zjRgstDate, zjExpDate, qyType, qyClass, qySize, RegAddr, PostAddr,
 * MailAddr。共 9 字段，全部 {@code minOccurs="0"}（XSD 层全部可选）。</p>
 *
 * <p>该 complexType 仅出现在 3102 报文的 {@link ArchiveInfo3102#getRzqyBaseInfo()}
 * 字段，scope 局限于 supplychain 包，不复用为 body.common。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rzqyBaseInfo")
@XmlType(name = "rzqyBaseInfo", propOrder = {
        "qyCreditCode", "zjRgstDate", "zjExpDate",
        "qyType", "qyClass", "qySize",
        "regAddr", "postAddr", "mailAddr"
})
public class RzqyBaseInfo extends CfxBody {

    @XmlElement(name = "qyCreditCode")
    private String qyCreditCode;

    @XmlElement(name = "zjRgstDate")
    private String zjRgstDate;

    @XmlElement(name = "zjExpDate")
    private String zjExpDate;

    @XmlElement(name = "qyType")
    private String qyType;

    @XmlElement(name = "qyClass")
    private String qyClass;

    @XmlElement(name = "qySize")
    private String qySize;

    @XmlElement(name = "RegAddr")
    private String regAddr;

    @XmlElement(name = "PostAddr")
    private String postAddr;

    @XmlElement(name = "MailAddr")
    private String mailAddr;

    /**
     * Returns the enterprise financing-credit code (企业融资中征码, optional).
     *
     * @return enterprise financing-credit code, or {@code null} if absent
     */
    public String getQyCreditCode() {
        return qyCreditCode;
    }

    /**
     * Sets the enterprise financing-credit code.
     *
     * @param v enterprise financing-credit code
     */
    public void setQyCreditCode(final String v) {
        this.qyCreditCode = v;
    }

    /**
     * Returns the certificate registration date (证件注册日, optional, yyyyMMdd).
     *
     * @return certificate registration date, or {@code null} if absent
     */
    public String getZjRgstDate() {
        return zjRgstDate;
    }

    /**
     * Sets the certificate registration date.
     *
     * @param v certificate registration date (yyyyMMdd)
     */
    public void setZjRgstDate(final String v) {
        this.zjRgstDate = v;
    }

    /**
     * Returns the certificate expiry date (证件到期日, optional, yyyyMMdd).
     *
     * @return certificate expiry date, or {@code null} if absent
     */
    public String getZjExpDate() {
        return zjExpDate;
    }

    /**
     * Sets the certificate expiry date.
     *
     * @param v certificate expiry date (yyyyMMdd)
     */
    public void setZjExpDate(final String v) {
        this.zjExpDate = v;
    }

    /**
     * Returns the enterprise type code (企业类型, optional, 1-2 digits).
     *
     * @return enterprise type code, or {@code null} if absent
     */
    public String getQyType() {
        return qyType;
    }

    /**
     * Sets the enterprise type code.
     *
     * @param v enterprise type code
     */
    public void setQyType(final String v) {
        this.qyType = v;
    }

    /**
     * Returns the industry classification code (行业分类代码, optional).
     *
     * @return industry classification code, or {@code null} if absent
     */
    public String getQyClass() {
        return qyClass;
    }

    /**
     * Sets the industry classification code.
     *
     * @param v industry classification code
     */
    public void setQyClass(final String v) {
        this.qyClass = v;
    }

    /**
     * Returns the enterprise size code (企业规模代码, optional, 4 chars).
     *
     * @return enterprise size code, or {@code null} if absent
     */
    public String getQySize() {
        return qySize;
    }

    /**
     * Sets the enterprise size code.
     *
     * @param v enterprise size code
     */
    public void setQySize(final String v) {
        this.qySize = v;
    }

    /**
     * Returns the registered address (注册地址, optional).
     *
     * @return registered address, or {@code null} if absent
     */
    public String getRegAddr() {
        return regAddr;
    }

    /**
     * Sets the registered address.
     *
     * @param v registered address
     */
    public void setRegAddr(final String v) {
        this.regAddr = v;
    }

    /**
     * Returns the postal/mailing address (通讯地址, optional).
     *
     * @return postal address, or {@code null} if absent
     */
    public String getPostAddr() {
        return postAddr;
    }

    /**
     * Sets the postal/mailing address.
     *
     * @param v postal address
     */
    public void setPostAddr(final String v) {
        this.postAddr = v;
    }

    /**
     * Returns the email address (电子邮件, optional).
     *
     * @return email address, or {@code null} if absent
     */
    public String getMailAddr() {
        return mailAddr;
    }

    /**
     * Sets the email address.
     *
     * @param v email address
     */
    public void setMailAddr(final String v) {
        this.mailAddr = v;
    }
}
