package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 1004 企业信息查询授权书发送报文业务体。
 *
 * <p>字段顺序严格对应 {@code 1004.xsd} 中 {@code CompanyAuthFileTransfer1004} complexType 的 sequence：
 * CompanyName, CompanyCode, AuthBeginDate, AuthEndDate, AuthNo, AuthOrgCode, IsUpdate?, Parameters?。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>本类仅承载"业务请求体"，不含 {@code RealHead1004}（由 P1b
 * {@link com.puchain.fep.converter.model.RequestBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyAuthFileTransfer1004")
@XmlType(propOrder = {
        "companyName", "companyCode", "authBeginDate", "authEndDate",
        "authNo", "authOrgCode", "isUpdate", "parameters"
})
public class CompanyAuthFileTransfer1004 extends CfxBody {

    @XmlElement(name = "CompanyName", required = true)
    private String companyName;

    @XmlElement(name = "CompanyCode", required = true)
    private String companyCode;

    @XmlElement(name = "AuthBeginDate", required = true)
    private String authBeginDate;

    @XmlElement(name = "AuthEndDate", required = true)
    private String authEndDate;

    @XmlElement(name = "AuthNo", required = true)
    private String authNo;

    @XmlElement(name = "AuthOrgCode", required = true)
    private String authOrgCode;

    @XmlElement(name = "IsUpdate")
    private String isUpdate;

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

    public String getAuthBeginDate() {
        return authBeginDate;
    }

    public void setAuthBeginDate(final String v) {
        this.authBeginDate = v;
    }

    public String getAuthEndDate() {
        return authEndDate;
    }

    public void setAuthEndDate(final String v) {
        this.authEndDate = v;
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

    public String getIsUpdate() {
        return isUpdate;
    }

    public void setIsUpdate(final String v) {
        this.isUpdate = v;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(final String v) {
        this.parameters = v;
    }
}
