package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code zpzAllInfo} — full certificate (凭证) summary record.
 *
 * <p>Fields follow the XSD {@code zpzAllInfo} complexType sequence (17 fields):
 * SerialNumber, pzNo, pzClass, PreNo, qyAssignName, qyAssignCode,
 * qyRecvName, qyRecvCode, Amt, UpdateDate, pzFunction, pzState,
 * pzrzState, pzMajorNo, LoanAmt, SubState, Reserve.</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "zpzAllInfo")
@XmlType(propOrder = {
        "serialNumber", "pzNo", "pzClass", "preNo",
        "qyAssignName", "qyAssignCode", "qyRecvName", "qyRecvCode",
        "amt", "updateDate", "pzFunction", "pzState",
        "pzrzState", "pzMajorNo", "loanAmt", "subState", "reserve"
})
public class ZpzAllInfo extends CfxBody {

    @XmlElement(name = "SerialNumber", required = true)
    private String serialNumber;

    @XmlElement(name = "pzNo", required = true)
    private String pzNo;

    @XmlElement(name = "pzClass", required = true)
    private String pzClass;

    @XmlElement(name = "PreNo")
    private String preNo;

    @XmlElement(name = "qyAssignName", required = true)
    private String qyAssignName;

    @XmlElement(name = "qyAssignCode", required = true)
    private String qyAssignCode;

    @XmlElement(name = "qyRecvName", required = true)
    private String qyRecvName;

    @XmlElement(name = "qyRecvCode", required = true)
    private String qyRecvCode;

    @XmlElement(name = "Amt", required = true)
    private String amt;

    @XmlElement(name = "UpdateDate", required = true)
    private String updateDate;

    @XmlElement(name = "pzFunction", required = true)
    private String pzFunction;

    @XmlElement(name = "pzState", required = true)
    private String pzState;

    @XmlElement(name = "pzrzState", required = true)
    private String pzrzState;

    @XmlElement(name = "pzMajorNo", required = true)
    private String pzMajorNo;

    @XmlElement(name = "LoanAmt", required = true)
    private String loanAmt;

    @XmlElement(name = "SubState", required = true)
    private String subState;

    @XmlElement(name = "Reserve")
    private String reserve;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(final String v) {
        this.serialNumber = v;
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

    public String getPreNo() {
        return preNo;
    }

    public void setPreNo(final String v) {
        this.preNo = v;
    }

    public String getQyAssignName() {
        return qyAssignName;
    }

    public void setQyAssignName(final String v) {
        this.qyAssignName = v;
    }

    public String getQyAssignCode() {
        return qyAssignCode;
    }

    public void setQyAssignCode(final String v) {
        this.qyAssignCode = v;
    }

    public String getQyRecvName() {
        return qyRecvName;
    }

    public void setQyRecvName(final String v) {
        this.qyRecvName = v;
    }

    public String getQyRecvCode() {
        return qyRecvCode;
    }

    public void setQyRecvCode(final String v) {
        this.qyRecvCode = v;
    }

    public String getAmt() {
        return amt;
    }

    public void setAmt(final String v) {
        this.amt = v;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(final String v) {
        this.updateDate = v;
    }

    public String getPzFunction() {
        return pzFunction;
    }

    public void setPzFunction(final String v) {
        this.pzFunction = v;
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

    public String getPzMajorNo() {
        return pzMajorNo;
    }

    public void setPzMajorNo(final String v) {
        this.pzMajorNo = v;
    }

    public String getLoanAmt() {
        return loanAmt;
    }

    public void setLoanAmt(final String v) {
        this.loanAmt = v;
    }

    public String getSubState() {
        return subState;
    }

    public void setSubState(final String v) {
        this.subState = v;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(final String v) {
        this.reserve = v;
    }
}
