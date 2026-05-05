package com.puchain.fep.processor.body.batch;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 1103 报文中 {@code CompanyInfoRequest} 单查询项。
 *
 * <p>字段顺序严格对应 {@code 1103.xsd} 中 {@code CompanyInfoRequest} complexType 的 sequence：
 * ItemId, CompanyName, CompanyCode, MainClass, SecondClass, BeginDate?, EndDate?,
 * AuthNo, AuthOrgCode, Parameters?。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompanyInfoRequest", propOrder = {
        "itemId", "companyName", "companyCode", "mainClass", "secondClass",
        "beginDate", "endDate", "authNo", "authOrgCode", "parameters"
})
public class CompanyInfoBatchItem1103 {

    @XmlElement(name = "ItemId", required = true)
    private String itemId;

    @XmlElement(name = "CompanyName", required = true)
    private String companyName;

    @XmlElement(name = "CompanyCode", required = true)
    private String companyCode;

    @XmlElement(name = "MainClass", required = true)
    private String mainClass;

    @XmlElement(name = "SecondClass", required = true)
    private String secondClass;

    @XmlElement(name = "BeginDate")
    private String beginDate;

    @XmlElement(name = "EndDate")
    private String endDate;

    @XmlElement(name = "AuthNo", required = true)
    private String authNo;

    @XmlElement(name = "AuthOrgCode", required = true)
    private String authOrgCode;

    @XmlElement(name = "Parameters")
    private String parameters;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(final String v) {
        this.itemId = v;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(final String v) {
        this.companyName = v;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(final String v) {
        this.companyCode = v;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(final String v) {
        this.mainClass = v;
    }

    public String getSecondClass() {
        return secondClass;
    }

    public void setSecondClass(final String v) {
        this.secondClass = v;
    }

    public String getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(final String v) {
        this.beginDate = v;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String v) {
        this.endDate = v;
    }

    public String getAuthNo() {
        return authNo;
    }

    public void setAuthNo(final String v) {
        this.authNo = v;
    }

    public String getAuthOrgCode() {
        return authOrgCode;
    }

    public void setAuthOrgCode(final String v) {
        this.authOrgCode = v;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(final String v) {
        this.parameters = v;
    }
}
