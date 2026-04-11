package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 1001 企业信息实时查询请求报文业务体。
 *
 * <p>字段顺序严格对应 {@code 1001.xsd} 中 {@code CompanyInfoRequest1001} complexType 的 sequence：
 * CompanyName, CompanyCode, MainClass, SecondClass, BeginDate?, EndDate?, AuthNo, AuthOrgCode, Parameters?。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>本类仅承载"业务请求体"，不含 {@code RealHead1001}（由 P1b
 * {@link com.puchain.fep.converter.model.RequestBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyInfoRequest1001")
@XmlType(propOrder = {
        "companyName", "companyCode", "mainClass", "secondClass",
        "beginDate", "endDate", "authNo", "authOrgCode", "parameters"
})
public class CompanyInfoRequest1001 extends CfxBody {

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
