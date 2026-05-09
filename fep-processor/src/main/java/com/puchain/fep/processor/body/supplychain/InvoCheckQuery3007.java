package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3007 发票核验请求报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3007.xsd} 中 {@code InvoCheckQuery3007} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, InvoCode, InvoNum, CheckCode, InvoAmtTax,
 * InvoAmt, InvoDate, ywKeyValue, ExtInfo?。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；{@link ExtInfo} 为可选嵌套扩展块。
 * XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "InvoCheckQuery3007")
@XmlType(name = "InvoCheckQuery3007", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "invoCode", "invoNum", "checkCode",
        "invoAmtTax", "invoAmt", "invoDate",
        "ywKeyValue", "extInfo"
})
public class InvoCheckQuery3007 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "InvoCode")
    private String invoCode;

    @XmlElement(name = "InvoNum", required = true)
    private String invoNum;

    @XmlElement(name = "CheckCode")
    private String checkCode;

    @XmlElement(name = "InvoAmtTax")
    private String invoAmtTax;

    @XmlElement(name = "InvoAmt", required = true)
    private String invoAmt;

    @XmlElement(name = "InvoDate", required = true)
    private String invoDate;

    @XmlElement(name = "ywKeyValue")
    private String ywKeyValue;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    /**
     * Returns the serial number.
     *
     * @return serial number
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the serial number.
     *
     * @param v serial number
     */
    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    /**
     * Returns the sending node code (14-char).
     *
     * @return sending node code
     */
    public String getSendNodeCode() {
        return sendNodeCode;
    }

    /**
     * Sets the sending node code.
     *
     * @param v sending node code
     */
    public void setSendNodeCode(final String v) {
        this.sendNodeCode = v;
    }

    /**
     * Returns the destination node code (14-char).
     *
     * @return destination node code
     */
    public String getDesNodeCode() {
        return desNodeCode;
    }

    /**
     * Sets the destination node code.
     *
     * @param v destination node code
     */
    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    /**
     * Returns the invoice code (发票代码, optional).
     *
     * @return invoice code, or {@code null} if absent
     */
    public String getInvoCode() {
        return invoCode;
    }

    /**
     * Sets the invoice code.
     *
     * @param v invoice code
     */
    public void setInvoCode(final String v) {
        this.invoCode = v;
    }

    /**
     * Returns the invoice number (发票号码).
     *
     * @return invoice number
     */
    public String getInvoNum() {
        return invoNum;
    }

    /**
     * Sets the invoice number.
     *
     * @param v invoice number
     */
    public void setInvoNum(final String v) {
        this.invoNum = v;
    }

    /**
     * Returns the invoice check code (发票校验码, optional, 6-char).
     *
     * @return check code, or {@code null} if absent
     */
    public String getCheckCode() {
        return checkCode;
    }

    /**
     * Sets the invoice check code.
     *
     * @param v check code
     */
    public void setCheckCode(final String v) {
        this.checkCode = v;
    }

    /**
     * Returns the invoice amount with tax (发票金额含税, optional).
     *
     * @return invoice amount including tax, or {@code null} if absent
     */
    public String getInvoAmtTax() {
        return invoAmtTax;
    }

    /**
     * Sets the invoice amount with tax.
     *
     * @param v amount including tax
     */
    public void setInvoAmtTax(final String v) {
        this.invoAmtTax = v;
    }

    /**
     * Returns the invoice amount without tax (发票金额不含税).
     *
     * @return invoice amount excluding tax
     */
    public String getInvoAmt() {
        return invoAmt;
    }

    /**
     * Sets the invoice amount without tax.
     *
     * @param v amount excluding tax
     */
    public void setInvoAmt(final String v) {
        this.invoAmt = v;
    }

    /**
     * Returns the invoice issue date (发票开票日期, {@code yyyyMMdd}).
     *
     * @return invoice date
     */
    public String getInvoDate() {
        return invoDate;
    }

    /**
     * Sets the invoice issue date.
     *
     * @param v invoice date
     */
    public void setInvoDate(final String v) {
        this.invoDate = v;
    }

    /**
     * Returns the business key value (发票相关业务备注, optional).
     *
     * @return business key value, or {@code null} if absent
     */
    public String getYwKeyValue() {
        return ywKeyValue;
    }

    /**
     * Sets the business key value.
     *
     * @param v business key value
     */
    public void setYwKeyValue(final String v) {
        this.ywKeyValue = v;
    }

    /**
     * Returns the optional extension info block.
     *
     * @return extension info, or {@code null} if absent
     */
    public ExtInfo getExtInfo() {
        return extInfo;
    }

    /**
     * Sets the optional extension info block.
     *
     * @param v extension info
     */
    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
