package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code pzInfo} — full certificate (凭证) detail record.
 *
 * <p>Fields follow the XSD {@code pzInfo} complexType sequence (32 of 33 fields mapped).
 * {@code pzFlowInfo} (maxOccurs=10, complexType) is intentionally omitted — its nested
 * structure is deferred to P2c; XSD validation still catches structural errors on raw XML.</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzInfo")
@XmlType(propOrder = {
        "platShortName", "platCode", "externalPlat",
        "hxqyName", "hxqyCode", "pzNo", "pzClass", "pzFunction",
        "klzrfName", "klzrfCode", "jsqyName", "jsqyCode", "jsqyPlatNo",
        "pzAmt", "pzStartDate", "pzEndDate", "pzState", "pzrzState", "pzFlowNum",
        "pzPreNo", "pzMajorNo", "pzrzSubAmt",
        "pzFilename", "signElement", "klzrfSign", "platSign",
        "remainQuota", "fxftRatio", "ffftRatio",
        "fkcnNo", "fkcnFile", "pzMemo"
})
public class PzInfo extends CfxBody {

    @XmlElement(name = "PlatShortName", required = true)
    private String platShortName;

    @XmlElement(name = "PlatCode", required = true)
    private String platCode;

    @XmlElement(name = "ExternalPlat", required = true)
    private String externalPlat;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "pzNo", required = true)
    private String pzNo;

    @XmlElement(name = "pzClass", required = true)
    private String pzClass;

    @XmlElement(name = "pzFunction", required = true)
    private String pzFunction;

    @XmlElement(name = "klzrfName", required = true)
    private String klzrfName;

    @XmlElement(name = "klzrfCode", required = true)
    private String klzrfCode;

    @XmlElement(name = "jsqyName", required = true)
    private String jsqyName;

    @XmlElement(name = "jsqyCode", required = true)
    private String jsqyCode;

    @XmlElement(name = "jsqyPlatNo", required = true)
    private String jsqyPlatNo;

    @XmlElement(name = "pzAmt", required = true)
    private String pzAmt;

    @XmlElement(name = "pzStartDate", required = true)
    private String pzStartDate;

    @XmlElement(name = "pzEndDate", required = true)
    private String pzEndDate;

    @XmlElement(name = "pzState", required = true)
    private String pzState;

    @XmlElement(name = "pzrzState", required = true)
    private String pzrzState;

    @XmlElement(name = "pzFlowNum", required = true)
    private String pzFlowNum;

    // pzFlowInfo (maxOccurs=10, complexType) omitted — deferred to P2c

    @XmlElement(name = "pzPreNo")
    private String pzPreNo;

    @XmlElement(name = "pzMajorNo")
    private String pzMajorNo;

    @XmlElement(name = "pzrzSubAmt")
    private String pzrzSubAmt;

    @XmlElement(name = "pzFilename")
    private String pzFilename;

    @XmlElement(name = "SignElement")
    private String signElement;

    @XmlElement(name = "klzrfSign")
    private String klzrfSign;

    @XmlElement(name = "PlatSign")
    private String platSign;

    @XmlElement(name = "RemainQuota")
    private String remainQuota;

    @XmlElement(name = "fxftRatio")
    private String fxftRatio;

    @XmlElement(name = "ffftRatio")
    private String ffftRatio;

    @XmlElement(name = "fkcnNo")
    private String fkcnNo;

    @XmlElement(name = "fkcnFile")
    private String fkcnFile;

    @XmlElement(name = "pzMemo")
    private String pzMemo;

    public String getPlatShortName() {
        return platShortName;
    }

    public void setPlatShortName(final String v) {
        this.platShortName = v;
    }

    public String getPlatCode() {
        return platCode;
    }

    public void setPlatCode(final String v) {
        this.platCode = v;
    }

    public String getExternalPlat() {
        return externalPlat;
    }

    public void setExternalPlat(final String v) {
        this.externalPlat = v;
    }

    public String getHxqyName() {
        return hxqyName;
    }

    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    public String getHxqyCode() {
        return hxqyCode;
    }

    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    public String getPzNo() {
        return pzNo;
    }

    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    public String getPzClass() {
        return pzClass;
    }

    public void setPzClass(final String v) {
        this.pzClass = v;
    }

    public String getPzFunction() {
        return pzFunction;
    }

    public void setPzFunction(final String v) {
        this.pzFunction = v;
    }

    public String getKlzrfName() {
        return klzrfName;
    }

    public void setKlzrfName(final String v) {
        this.klzrfName = v;
    }

    public String getKlzrfCode() {
        return klzrfCode;
    }

    public void setKlzrfCode(final String v) {
        this.klzrfCode = v;
    }

    public String getJsqyName() {
        return jsqyName;
    }

    public void setJsqyName(final String v) {
        this.jsqyName = v;
    }

    public String getJsqyCode() {
        return jsqyCode;
    }

    public void setJsqyCode(final String v) {
        this.jsqyCode = v;
    }

    public String getJsqyPlatNo() {
        return jsqyPlatNo;
    }

    public void setJsqyPlatNo(final String v) {
        this.jsqyPlatNo = v;
    }

    public String getPzAmt() {
        return pzAmt;
    }

    public void setPzAmt(final String v) {
        this.pzAmt = v;
    }

    public String getPzStartDate() {
        return pzStartDate;
    }

    public void setPzStartDate(final String v) {
        this.pzStartDate = v;
    }

    public String getPzEndDate() {
        return pzEndDate;
    }

    public void setPzEndDate(final String v) {
        this.pzEndDate = v;
    }

    public String getPzState() {
        return pzState;
    }

    public void setPzState(final String v) {
        this.pzState = v;
    }

    public String getPzrzState() {
        return pzrzState;
    }

    public void setPzrzState(final String v) {
        this.pzrzState = v;
    }

    public String getPzFlowNum() {
        return pzFlowNum;
    }

    public void setPzFlowNum(final String v) {
        this.pzFlowNum = v;
    }

    public String getPzPreNo() {
        return pzPreNo;
    }

    public void setPzPreNo(final String v) {
        this.pzPreNo = v;
    }

    public String getPzMajorNo() {
        return pzMajorNo;
    }

    public void setPzMajorNo(final String v) {
        this.pzMajorNo = v;
    }

    public String getPzrzSubAmt() {
        return pzrzSubAmt;
    }

    public void setPzrzSubAmt(final String v) {
        this.pzrzSubAmt = v;
    }

    public String getPzFilename() {
        return pzFilename;
    }

    public void setPzFilename(final String v) {
        this.pzFilename = v;
    }

    public String getSignElement() {
        return signElement;
    }

    public void setSignElement(final String v) {
        this.signElement = v;
    }

    public String getKlzrfSign() {
        return klzrfSign;
    }

    public void setKlzrfSign(final String v) {
        this.klzrfSign = v;
    }

    public String getPlatSign() {
        return platSign;
    }

    public void setPlatSign(final String v) {
        this.platSign = v;
    }

    public String getRemainQuota() {
        return remainQuota;
    }

    public void setRemainQuota(final String v) {
        this.remainQuota = v;
    }

    public String getFxftRatio() {
        return fxftRatio;
    }

    public void setFxftRatio(final String v) {
        this.fxftRatio = v;
    }

    public String getFfftRatio() {
        return ffftRatio;
    }

    public void setFfftRatio(final String v) {
        this.ffftRatio = v;
    }

    public String getFkcnNo() {
        return fkcnNo;
    }

    public void setFkcnNo(final String v) {
        this.fkcnNo = v;
    }

    public String getFkcnFile() {
        return fkcnFile;
    }

    public void setFkcnFile(final String v) {
        this.fkcnFile = v;
    }

    public String getPzMemo() {
        return pzMemo;
    }

    public void setPzMemo(final String v) {
        this.pzMemo = v;
    }
}
