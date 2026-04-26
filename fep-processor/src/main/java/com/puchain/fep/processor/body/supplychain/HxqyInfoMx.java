package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code hxqyInfoMx} — 核心企业登记信息明细（11 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code hxqyInfoMx} complexType 的 sequence:
 * PlatNodeCode, hxqyName, hxqyCode, hxqyState (required) +
 * hxqyClass, hxqyDateBegin, hxqyDateEnd, hxqyGroupName, hxqyGroupCode,
 * BankCodeList, hxqyCAFilename (optional)。</p>
 *
 * <p>本类作为 {@link HxqyInfo3109#getHxqyInfoMx()} 的列表元素出现，
 * {@code maxOccurs="20"} 限制每条 3109 报文最多 20 个核心企业明细。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hxqyInfoMx")
@XmlType(name = "hxqyInfoMx", propOrder = {
        "platNodeCode", "hxqyName", "hxqyCode", "hxqyState",
        "hxqyClass", "hxqyDateBegin", "hxqyDateEnd",
        "hxqyGroupName", "hxqyGroupCode", "bankCodeList", "hxqyCAFilename"
})
public class HxqyInfoMx extends CfxBody {

    @XmlElement(name = "PlatNodeCode", required = true)
    private String platNodeCode;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "hxqyState", required = true)
    private String hxqyState;

    @XmlElement(name = "hxqyClass")
    private String hxqyClass;

    @XmlElement(name = "hxqyDateBegin")
    private String hxqyDateBegin;

    @XmlElement(name = "hxqyDateEnd")
    private String hxqyDateEnd;

    @XmlElement(name = "hxqyGroupName")
    private String hxqyGroupName;

    @XmlElement(name = "hxqyGroupCode")
    private String hxqyGroupCode;

    @XmlElement(name = "BankCodeList")
    private String bankCodeList;

    @XmlElement(name = "hxqyCAFilename")
    private String hxqyCAFilename;

    /**
     * Returns the platform node code where the core enterprise resides
     * (核心企业所在平台节点代码, 14-char).
     *
     * @return platform node code
     */
    public String getPlatNodeCode() {
        return platNodeCode;
    }

    /**
     * Sets the platform node code where the core enterprise resides.
     *
     * @param v platform node code
     */
    public void setPlatNodeCode(final String v) {
        this.platNodeCode = v;
    }

    /**
     * Returns the core enterprise name (核心企业名称).
     *
     * @return core enterprise name
     */
    public String getHxqyName() {
        return hxqyName;
    }

    /**
     * Sets the core enterprise name.
     *
     * @param v core enterprise name
     */
    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    /**
     * Returns the core enterprise unified social credit code (核心企业统一社会信用代码, 18-char USCI).
     *
     * @return core enterprise USCI
     */
    public String getHxqyCode() {
        return hxqyCode;
    }

    /**
     * Sets the core enterprise unified social credit code.
     *
     * @param v core enterprise USCI
     */
    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    /**
     * Returns the core enterprise registration state code (核心企业登记状态代码).
     *
     * @return registration state code
     */
    public String getHxqyState() {
        return hxqyState;
    }

    /**
     * Sets the core enterprise registration state code.
     *
     * @param v registration state code
     */
    public void setHxqyState(final String v) {
        this.hxqyState = v;
    }

    /**
     * Returns the optional business class code (核心企业登记业务代码).
     *
     * @return class code, or {@code null} if absent
     */
    public String getHxqyClass() {
        return hxqyClass;
    }

    /**
     * Sets the business class code.
     *
     * @param v class code
     */
    public void setHxqyClass(final String v) {
        this.hxqyClass = v;
    }

    /**
     * Returns the optional registration online date (核心企业登记上线日期, yyyyMMdd).
     *
     * @return begin date, or {@code null} if absent
     */
    public String getHxqyDateBegin() {
        return hxqyDateBegin;
    }

    /**
     * Sets the registration online date.
     *
     * @param v begin date (yyyyMMdd)
     */
    public void setHxqyDateBegin(final String v) {
        this.hxqyDateBegin = v;
    }

    /**
     * Returns the optional registration end date (核心企业截止日期, yyyyMMdd).
     *
     * @return end date, or {@code null} if absent
     */
    public String getHxqyDateEnd() {
        return hxqyDateEnd;
    }

    /**
     * Sets the registration end date.
     *
     * @param v end date (yyyyMMdd)
     */
    public void setHxqyDateEnd(final String v) {
        this.hxqyDateEnd = v;
    }

    /**
     * Returns the optional parent group name (核心企业所属集团名称).
     *
     * @return group name, or {@code null} if absent
     */
    public String getHxqyGroupName() {
        return hxqyGroupName;
    }

    /**
     * Sets the parent group name.
     *
     * @param v group name
     */
    public void setHxqyGroupName(final String v) {
        this.hxqyGroupName = v;
    }

    /**
     * Returns the optional parent group USCI (核心企业所属集团社会信用代码).
     *
     * @return group USCI, or {@code null} if absent
     */
    public String getHxqyGroupCode() {
        return hxqyGroupCode;
    }

    /**
     * Sets the parent group USCI.
     *
     * @param v group USCI
     */
    public void setHxqyGroupCode(final String v) {
        this.hxqyGroupCode = v;
    }

    /**
     * Returns the optional associated bank code list (核心企业关联银行代码).
     *
     * @return bank code list, or {@code null} if absent
     */
    public String getBankCodeList() {
        return bankCodeList;
    }

    /**
     * Sets the associated bank code list.
     *
     * @param v bank code list
     */
    public void setBankCodeList(final String v) {
        this.bankCodeList = v;
    }

    /**
     * Returns the optional public-key certificate file name (核心企业公钥证书文件名).
     *
     * @return certificate file name, or {@code null} if absent
     */
    public String getHxqyCAFilename() {
        return hxqyCAFilename;
    }

    /**
     * Sets the public-key certificate file name.
     *
     * @param v certificate file name
     */
    public void setHxqyCAFilename(final String v) {
        this.hxqyCAFilename = v;
    }
}
