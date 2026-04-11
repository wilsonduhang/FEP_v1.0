package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 2001 企业信息实时查询回执报文业务体。
 *
 * <p>字段顺序严格对应 {@code 2001.xsd} 中 {@code CompanyInfoResponse2001} complexType 的 sequence：
 * CompanyName, CompanyCode, MainClass, SecondClass, BeginDate?, EndDate?, QueryResult, QueryAddWord?。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制。</p>
 *
 * <p>本类仅承载"业务回执体"，不含 {@code RealHead2001}（由 P1b
 * {@link com.puchain.fep.converter.model.ResponseBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyInfoResponse2001")
@XmlType(propOrder = {
        "companyName", "companyCode", "mainClass", "secondClass",
        "beginDate", "endDate", "queryResult", "queryAddWord"
})
public class CompanyInfoResponse2001 extends CfxBody {

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

    @XmlElement(name = "QueryResult", required = true)
    private String queryResult;

    @XmlElement(name = "QueryAddWord")
    private String queryAddWord;

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
