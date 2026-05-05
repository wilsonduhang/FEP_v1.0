package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 2103 报文中 {@code CompanyInfo} 单查询结果项。
 *
 * <p>字段顺序严格对应 {@code 2103.xsd} 中 {@code CompanyInfo} complexType 的 sequence：
 * ItemId, CompanyName, CompanyCode, MainClass, SecondClass, BeginDate?, EndDate?,
 * AuthOrgCode, FileName?, QueryResult, QueryAddWord?。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制。</p>
 *
 * <p>vs 1103 item 差异：无 {@code AuthNo} / {@code Parameters}，多
 * {@code FileName} / {@code QueryResult} / {@code QueryAddWord}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompanyInfo", propOrder = {
        "itemId", "companyName", "companyCode", "mainClass", "secondClass",
        "beginDate", "endDate", "authOrgCode", "fileName", "queryResult", "queryAddWord"
})
public class CompanyInfoBatchItem2103 extends CfxBody {

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

    @XmlElement(name = "AuthOrgCode", required = true)
    private String authOrgCode;

    @XmlElement(name = "FileName")
    private String fileName;

    @XmlElement(name = "QueryResult", required = true)
    private String queryResult;

    @XmlElement(name = "QueryAddWord")
    private String queryAddWord;

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

    public String getAuthOrgCode() {
        return authOrgCode;
    }

    public void setAuthOrgCode(final String v) {
        this.authOrgCode = v;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String v) {
        this.fileName = v;
    }

    public String getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(final String v) {
        this.queryResult = v;
    }

    public String getQueryAddWord() {
        return queryAddWord;
    }

    public void setQueryAddWord(final String v) {
        this.queryAddWord = v;
    }
}
