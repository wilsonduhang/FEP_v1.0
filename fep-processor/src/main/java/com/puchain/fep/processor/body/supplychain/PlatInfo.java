package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.PersonInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * Local complexType {@code PlatInfo} — 平台企业登记信息段（22 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code PlatInfo} complexType 的 sequence:
 * PlatNodeCode, PlatName, PlatCode, PlatState, PlatType, PlatSysName, PlatSysShortName,
 * PlatServiceObject, PlatDevelopmentMethod, SAASServiceName?, PlatSysSAAS, OtherService,
 * PlatSysURL?, PlatRegAddr?, PlatRegAmt, PlatPaidinAmt, PlatParent?, PlatDateBegin,
 * PlatDateEnd?, PlatCAFilename?, ShareholderInfo[1..10], ContactPersonInfo (PersonInfo)。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link ShareholderInfo}{@code >} — 主要股东信息列表
 *       （{@code maxOccurs="10"}, required, T5 新建于本包）</li>
 *   <li>{@link PersonInfo} {@code ContactPersonInfo} — 联系人信息
 *       （required, body.common P2d-ext T0.5 已落地）</li>
 * </ul></p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}（含 Boolean 字段 PlatSysSAAS / OtherService
 * 和有 default 值的 PlatType）；XSD 长度/格式/枚举/默认值约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>本类 22 字段同 {@link com.puchain.fep.processor.body.common.PzInfo} 的 33 字段一样，
 * 因 getter/setter 数量较多采用与 {@code PzInfo} 一致的 Javadoc 简写惯例：
 * 仅保留类与字段级语义注释，省略 getter/setter Javadoc。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PlatInfo")
@XmlType(name = "PlatInfo", propOrder = {
        "platNodeCode", "platName", "platCode", "platState", "platType",
        "platSysName", "platSysShortName", "platServiceObject", "platDevelopmentMethod",
        "saasServiceName", "platSysSAAS", "otherService",
        "platSysURL", "platRegAddr", "platRegAmt", "platPaidinAmt", "platParent",
        "platDateBegin", "platDateEnd", "platCAFilename",
        "shareholderInfo", "contactPersonInfo"
})
public class PlatInfo extends CfxBody {

    @XmlElement(name = "PlatNodeCode", required = true)
    private String platNodeCode;

    @XmlElement(name = "PlatName", required = true)
    private String platName;

    @XmlElement(name = "PlatCode", required = true)
    private String platCode;

    @XmlElement(name = "PlatState", required = true)
    private String platState;

    @XmlElement(name = "PlatType", required = true)
    private String platType;

    @XmlElement(name = "PlatSysName", required = true)
    private String platSysName;

    @XmlElement(name = "PlatSysShortName", required = true)
    private String platSysShortName;

    @XmlElement(name = "PlatServiceObject", required = true)
    private String platServiceObject;

    @XmlElement(name = "PlatDevelopmentMethod", required = true)
    private String platDevelopmentMethod;

    @XmlElement(name = "SAASServiceName")
    private String saasServiceName;

    @XmlElement(name = "PlatSysSAAS", required = true)
    private String platSysSAAS;

    @XmlElement(name = "OtherService", required = true)
    private String otherService;

    @XmlElement(name = "PlatSysURL")
    private String platSysURL;

    @XmlElement(name = "PlatRegAddr")
    private String platRegAddr;

    @XmlElement(name = "PlatRegAmt", required = true)
    private String platRegAmt;

    @XmlElement(name = "PlatPaidinAmt", required = true)
    private String platPaidinAmt;

    @XmlElement(name = "PlatParent")
    private String platParent;

    @XmlElement(name = "PlatDateBegin", required = true)
    private String platDateBegin;

    @XmlElement(name = "PlatDateEnd")
    private String platDateEnd;

    @XmlElement(name = "PlatCAFilename")
    private String platCAFilename;

    @XmlElement(name = "ShareholderInfo", required = true)
    private List<ShareholderInfo> shareholderInfo;

    @XmlElement(name = "ContactPersonInfo", required = true)
    private PersonInfo contactPersonInfo;

    public String getPlatNodeCode() {
        return platNodeCode;
    }

    public void setPlatNodeCode(final String v) {
        this.platNodeCode = v;
    }

    public String getPlatName() {
        return platName;
    }

    public void setPlatName(final String v) {
        this.platName = v;
    }

    public String getPlatCode() {
        return platCode;
    }

    public void setPlatCode(final String v) {
        this.platCode = v;
    }

    public String getPlatState() {
        return platState;
    }

    public void setPlatState(final String v) {
        this.platState = v;
    }

    public String getPlatType() {
        return platType;
    }

    public void setPlatType(final String v) {
        this.platType = v;
    }

    public String getPlatSysName() {
        return platSysName;
    }

    public void setPlatSysName(final String v) {
        this.platSysName = v;
    }

    public String getPlatSysShortName() {
        return platSysShortName;
    }

    public void setPlatSysShortName(final String v) {
        this.platSysShortName = v;
    }

    public String getPlatServiceObject() {
        return platServiceObject;
    }

    public void setPlatServiceObject(final String v) {
        this.platServiceObject = v;
    }

    public String getPlatDevelopmentMethod() {
        return platDevelopmentMethod;
    }

    public void setPlatDevelopmentMethod(final String v) {
        this.platDevelopmentMethod = v;
    }

    public String getSaasServiceName() {
        return saasServiceName;
    }

    public void setSaasServiceName(final String v) {
        this.saasServiceName = v;
    }

    public String getPlatSysSAAS() {
        return platSysSAAS;
    }

    public void setPlatSysSAAS(final String v) {
        this.platSysSAAS = v;
    }

    public String getOtherService() {
        return otherService;
    }

    public void setOtherService(final String v) {
        this.otherService = v;
    }

    public String getPlatSysURL() {
        return platSysURL;
    }

    public void setPlatSysURL(final String v) {
        this.platSysURL = v;
    }

    public String getPlatRegAddr() {
        return platRegAddr;
    }

    public void setPlatRegAddr(final String v) {
        this.platRegAddr = v;
    }

    public String getPlatRegAmt() {
        return platRegAmt;
    }

    public void setPlatRegAmt(final String v) {
        this.platRegAmt = v;
    }

    public String getPlatPaidinAmt() {
        return platPaidinAmt;
    }

    public void setPlatPaidinAmt(final String v) {
        this.platPaidinAmt = v;
    }

    public String getPlatParent() {
        return platParent;
    }

    public void setPlatParent(final String v) {
        this.platParent = v;
    }

    public String getPlatDateBegin() {
        return platDateBegin;
    }

    public void setPlatDateBegin(final String v) {
        this.platDateBegin = v;
    }

    public String getPlatDateEnd() {
        return platDateEnd;
    }

    public void setPlatDateEnd(final String v) {
        this.platDateEnd = v;
    }

    public String getPlatCAFilename() {
        return platCAFilename;
    }

    public void setPlatCAFilename(final String v) {
        this.platCAFilename = v;
    }

    public List<ShareholderInfo> getShareholderInfo() {
        return shareholderInfo;
    }

    public void setShareholderInfo(final List<ShareholderInfo> v) {
        this.shareholderInfo = v;
    }

    public PersonInfo getContactPersonInfo() {
        return contactPersonInfo;
    }

    public void setContactPersonInfo(final PersonInfo v) {
        this.contactPersonInfo = v;
    }
}
