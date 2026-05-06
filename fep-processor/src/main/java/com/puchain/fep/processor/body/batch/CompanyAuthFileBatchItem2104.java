package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 2104 报文中 {@code CompanyAuthFileResponse} 单备案回执项。
 *
 * <p>字段顺序严格对应 {@code 2104.xsd} 中 {@code CompanyAuthFileResponse} complexType 的 sequence：
 * ItemId, CompanyName, CompanyCode, AuthBeginDate, AuthEndDate, AuthNo, AuthOrgCode,
 * IsUpdate?, RecordResult, RecordAddWord?。</p>
 *
 * <p><b>差异 vs 1104 {@link CompanyAuthFileBatchItem1104}：</b>
 * <ul>
 *   <li>{@code IsUpdate} 在 2104 XSD 显式 {@code type="Boolean"}（"0"/"1"），Java 仍用 String</li>
 *   <li>新增 {@code RecordResult}（5 位 Number，required，备案结果）+ {@code RecordAddWord}（optional 附言）</li>
 *   <li>无 {@code Parameters} 字段（1104 独有）</li>
 *   <li>无 {@code FileName} 字段（1104 sequence 末尾，2104 移除）</li>
 * </ul>
 * </p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompanyAuthFileResponse", propOrder = {
        "itemId", "companyName", "companyCode",
        "authBeginDate", "authEndDate", "authNo", "authOrgCode",
        "isUpdate", "recordResult", "recordAddWord"
})
public class CompanyAuthFileBatchItem2104 extends CfxBody {

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

    @XmlElement(name = "RecordResult", required = true)
    private String recordResult;

    @XmlElement(name = "RecordAddWord")
    private String recordAddWord;

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

    public String getRecordResult() {
        return recordResult;
    }

    public void setRecordResult(final String v) {
        this.recordResult = v;
    }

    public String getRecordAddWord() {
        return recordAddWord;
    }

    public void setRecordAddWord(final String v) {
        this.recordAddWord = v;
    }
}
