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
 * 3008 发票核验回执报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3008.xsd} 中 {@code InvoCheckReturn3008} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, InvoCheckReturnCode, InvoCheckReturnMemo?,
 * kpName, kpCode, spName, spCode?, InvoCode?, InvoNum?, CheckCode?,
 * InvoAmtTax?, InvoAmt?, InvoDate?, InvoFilename?, ExtInfo?。</p>
 *
 * <p>XSD complexType name 实测为 {@code InvoCheckReturn3008}（不是 Response）。
 * 所有文本字段 Java 类型统一为 {@link String}；{@link ExtInfo} 为可选嵌套扩展块。
 * XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "InvoCheckReturn3008")
@XmlType(name = "InvoCheckReturn3008", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "invoCheckReturnCode", "invoCheckReturnMemo",
        "kpName", "kpCode", "spName", "spCode",
        "invoCode", "invoNum", "checkCode",
        "invoAmtTax", "invoAmt", "invoDate",
        "invoFilename", "extInfo"
})
public class InvoCheckReturn3008 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "InvoCheckReturnCode", required = true)
    private String invoCheckReturnCode;

    @XmlElement(name = "InvoCheckReturnMemo")
    private String invoCheckReturnMemo;

    @XmlElement(name = "kpName", required = true)
    private String kpName;

    @XmlElement(name = "kpCode", required = true)
    private String kpCode;

    @XmlElement(name = "spName", required = true)
    private String spName;

    @XmlElement(name = "spCode")
    private String spCode;

    @XmlElement(name = "InvoCode")
    private String invoCode;

    @XmlElement(name = "InvoNum")
    private String invoNum;

    @XmlElement(name = "CheckCode")
    private String checkCode;

    @XmlElement(name = "InvoAmtTax")
    private String invoAmtTax;

    @XmlElement(name = "InvoAmt")
    private String invoAmt;

    @XmlElement(name = "InvoDate")
    private String invoDate;

    @XmlElement(name = "InvoFilename")
    private String invoFilename;

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
     * Returns the sending node code.
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
     * Returns the destination node code.
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
     * Returns the invoice check return code (查询返回码).
     *
     * @return return code
     */
    public String getInvoCheckReturnCode() {
        return invoCheckReturnCode;
    }

    /**
     * Sets the invoice check return code.
     *
     * @param v return code
     */
    public void setInvoCheckReturnCode(final String v) {
        this.invoCheckReturnCode = v;
    }

    /**
     * Returns the return memo (返回码备注, optional).
     *
     * @return return memo, or {@code null} if absent
     */
    public String getInvoCheckReturnMemo() {
        return invoCheckReturnMemo;
    }

    /**
     * Sets the return memo.
     *
     * @param v return memo
     */
    public void setInvoCheckReturnMemo(final String v) {
        this.invoCheckReturnMemo = v;
    }

    /**
     * Returns the seller (开票方) name.
     *
     * @return seller name
     */
    public String getKpName() {
        return kpName;
    }

    /**
     * Sets the seller name.
     *
     * @param v seller name
     */
    public void setKpName(final String v) {
        this.kpName = v;
    }

    /**
     * Returns the seller unified social credit code.
     *
     * @return seller code
     */
    public String getKpCode() {
        return kpCode;
    }

    /**
     * Sets the seller unified social credit code.
     *
     * @param v seller code
     */
    public void setKpCode(final String v) {
        this.kpCode = v;
    }

    /**
     * Returns the buyer (购买方) name.
     *
     * @return buyer name
     */
    public String getSpName() {
        return spName;
    }

    /**
     * Sets the buyer name.
     *
     * @param v buyer name
     */
    public void setSpName(final String v) {
        this.spName = v;
    }

    /**
     * Returns the buyer unified social credit code (optional).
     *
     * @return buyer code, or {@code null} if absent
     */
    public String getSpCode() {
        return spCode;
    }

    /**
     * Sets the buyer unified social credit code.
     *
     * @param v buyer code
     */
    public void setSpCode(final String v) {
        this.spCode = v;
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
     * Returns the invoice number (发票号码, optional).
     *
     * @return invoice number, or {@code null} if absent
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
     * Returns the invoice check code (发票校验码, optional).
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
     * Returns the invoice amount with tax (optional).
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
     * Returns the invoice amount without tax (optional).
     *
     * @return invoice amount excluding tax, or {@code null} if absent
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
     * Returns the invoice issue date (optional, {@code yyyyMMdd}).
     *
     * @return invoice date, or {@code null} if absent
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
     * Returns the invoice attachment filename (发票对应文件, optional).
     *
     * @return invoice filename, or {@code null} if absent
     */
    public String getInvoFilename() {
        return invoFilename;
    }

    /**
     * Sets the invoice attachment filename.
     *
     * @param v invoice filename
     */
    public void setInvoFilename(final String v) {
        this.invoFilename = v;
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
