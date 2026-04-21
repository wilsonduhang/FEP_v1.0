package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 凭证流转记录（pzFlowInfo complexType）。
 *
 * <p>Shared complexType defined in {@code DataType.xsd:1076-1137}, used as a
 * nested element (maxOccurs=10) inside {@code pzInfo} and therefore shared by
 * 3001/3004 and other supply-chain message bodies (PRD v1.3 §4.4).</p>
 *
 * <p>Fields follow the XSD sequence strictly: {@code SerialNumber}, {@code pzNo},
 * {@code PreNo}, {@code qyAssignName}, {@code qyAssignCode}, {@code qyRecvName},
 * {@code qyRecvCode}, {@code Amt}, {@code UpdateDate} (required) then
 * {@code AssignSign}, {@code FlowVoucherFile} (optional).</p>
 *
 * <p><strong>Decision</strong>: although {@code SerialNumber} is declared as
 * {@code xs:integer} in the XSD, this POJO exposes it as {@link String} —
 * consistent with other integer-typed fields in sibling body POJOs. Numeric
 * constraints are enforced by {@code XsdValidator} at the transport boundary
 * rather than by POJO typing, so JAXB round-tripping stays lossless.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzFlowInfo")
@XmlType(propOrder = {
        "serialNumber",
        "pzNo",
        "preNo",
        "qyAssignName",
        "qyAssignCode",
        "qyRecvName",
        "qyRecvCode",
        "amt",
        "updateDate",
        "assignSign",
        "flowVoucherFile"
})
public class PzFlowInfo extends CfxBody {

    @XmlElement(name = "SerialNumber", required = true)
    private String serialNumber;

    @XmlElement(name = "pzNo", required = true)
    private String pzNo;

    @XmlElement(name = "PreNo", required = true)
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

    @XmlElement(name = "AssignSign")
    private String assignSign;

    @XmlElement(name = "FlowVoucherFile")
    private String flowVoucherFile;

    /**
     * Returns the serial number of this flow record.
     *
     * @return the serial number (String form of {@code xs:integer})
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * Sets the serial number of this flow record.
     *
     * @param v the serial number
     */
    public void setSerialNumber(final String v) {
        this.serialNumber = v;
    }

    /**
     * Returns the voucher number (凭证编号).
     *
     * @return the voucher number
     */
    public String getPzNo() {
        return pzNo;
    }

    /**
     * Sets the voucher number (凭证编号).
     *
     * @param v the voucher number
     */
    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    /**
     * Returns the parent voucher number (上一级电子凭证编号).
     *
     * @return the parent voucher number
     */
    public String getPreNo() {
        return preNo;
    }

    /**
     * Sets the parent voucher number (上一级电子凭证编号).
     *
     * @param v the parent voucher number
     */
    public void setPreNo(final String v) {
        this.preNo = v;
    }

    /**
     * Returns the assignor enterprise name (转让方企业名称).
     *
     * @return the assignor name
     */
    public String getQyAssignName() {
        return qyAssignName;
    }

    /**
     * Sets the assignor enterprise name (转让方企业名称).
     *
     * @param v the assignor name
     */
    public void setQyAssignName(final String v) {
        this.qyAssignName = v;
    }

    /**
     * Returns the assignor USCI (转让方统一社会信用代码).
     *
     * @return the assignor USCI
     */
    public String getQyAssignCode() {
        return qyAssignCode;
    }

    /**
     * Sets the assignor USCI (转让方统一社会信用代码).
     *
     * @param v the assignor USCI
     */
    public void setQyAssignCode(final String v) {
        this.qyAssignCode = v;
    }

    /**
     * Returns the recipient enterprise name (接收方企业名称).
     *
     * @return the recipient name
     */
    public String getQyRecvName() {
        return qyRecvName;
    }

    /**
     * Sets the recipient enterprise name (接收方企业名称).
     *
     * @param v the recipient name
     */
    public void setQyRecvName(final String v) {
        this.qyRecvName = v;
    }

    /**
     * Returns the recipient USCI (接收方统一社会信用代码).
     *
     * @return the recipient USCI
     */
    public String getQyRecvCode() {
        return qyRecvCode;
    }

    /**
     * Sets the recipient USCI (接收方统一社会信用代码).
     *
     * @param v the recipient USCI
     */
    public void setQyRecvCode(final String v) {
        this.qyRecvCode = v;
    }

    /**
     * Returns the voucher amount (凭证金额).
     *
     * @return the voucher amount
     */
    public String getAmt() {
        return amt;
    }

    /**
     * Sets the voucher amount (凭证金额).
     *
     * @param v the voucher amount
     */
    public void setAmt(final String v) {
        this.amt = v;
    }

    /**
     * Returns the update date in {@code YYYYMMDD} form.
     *
     * @return the update date
     */
    public String getUpdateDate() {
        return updateDate;
    }

    /**
     * Sets the update date in {@code YYYYMMDD} form.
     *
     * @param v the update date
     */
    public void setUpdateDate(final String v) {
        this.updateDate = v;
    }

    /**
     * Returns the optional assignor PKCS#7 digital signature.
     *
     * @return the assignor signature, or {@code null} if not set
     */
    public String getAssignSign() {
        return assignSign;
    }

    /**
     * Sets the optional assignor PKCS#7 digital signature.
     *
     * @param v the assignor signature
     */
    public void setAssignSign(final String v) {
        this.assignSign = v;
    }

    /**
     * Returns the optional flow voucher file name.
     *
     * @return the flow voucher file name, or {@code null} if not set
     */
    public String getFlowVoucherFile() {
        return flowVoucherFile;
    }

    /**
     * Sets the optional flow voucher file name.
     *
     * @param v the flow voucher file name
     */
    public void setFlowVoucherFile(final String v) {
        this.flowVoucherFile = v;
    }
}
