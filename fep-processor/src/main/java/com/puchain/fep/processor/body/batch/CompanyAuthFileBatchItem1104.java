package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 1104 报文中 {@code CompanyAuthFile} 单授权书项。
 *
 * <p>字段顺序严格对应 {@code 1104.xsd} 中 {@code CompanyAuthFile} complexType 的 sequence：
 * ItemId, CompanyName, CompanyCode, AuthBeginDate, AuthEndDate, AuthNo, AuthOrgCode,
 * IsUpdate?, Parameters?, FileName。</p>
 *
 * <p><b>IsUpdate / Parameters 类型注：</b>1104 XSD 中这两个字段无 {@code type} 属性，按
 * XSD anyType 处理；本 POJO 用 {@link String} 承载，由 caller 保证业务语义（IsUpdate 通常
 * "0" / "1"）。2104 回执报文中 IsUpdate 显式标 type=Boolean，回执侧严格校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompanyAuthFile", propOrder = {
        "itemId", "companyName", "companyCode",
        "authBeginDate", "authEndDate", "authNo", "authOrgCode",
        "isUpdate", "parameters", "fileName"
})
public class CompanyAuthFileBatchItem1104 extends CfxBody {

    @XmlElement(name = "ItemId", required = true)
    private String itemId;

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

    @XmlElement(name = "FileName", required = true)
    private String fileName;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String v) {
        this.fileName = v;
    }
}
