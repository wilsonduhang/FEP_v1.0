package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code qsReturnInfo} — 清算回执信息（6 字段）。
 *
 * <p>字段顺序严格对应 {@code 3115.xsd} 中 {@code qsReturnInfo} complexType 的 sequence：
 * qsReturnBankName, qsReturnCode, qsReturnSerialNo, qsReturnDate,
 * qsReturnFilename?, qsReturnMemo?。前 4 字段 required；后 2 字段 optional
 * （{@code minOccurs="0"}）。</p>
 *
 * <p>嵌入在 {@link QsInfo}（{@code qsInfo.qsReturnInfo}, optional）下，作为银行执行
 * 清算指令后的回执回填块。仅 3115 报文使用本类型。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qsReturnInfo")
@XmlType(name = "qsReturnInfo", propOrder = {
        "qsReturnBankName", "qsReturnCode", "qsReturnSerialNo",
        "qsReturnDate", "qsReturnFilename", "qsReturnMemo"
})
public class QsReturnInfo extends CfxBody {

    @XmlElement(name = "qsReturnBankName", required = true)
    private String qsReturnBankName;

    @XmlElement(name = "qsReturnCode", required = true)
    private String qsReturnCode;

    @XmlElement(name = "qsReturnSerialNo", required = true)
    private String qsReturnSerialNo;

    @XmlElement(name = "qsReturnDate", required = true)
    private String qsReturnDate;

    @XmlElement(name = "qsReturnFilename")
    private String qsReturnFilename;

    @XmlElement(name = "qsReturnMemo")
    private String qsReturnMemo;

    /**
     * Returns the settling bank name (清算银行名称).
     *
     * @return settling bank name
     */
    public String getQsReturnBankName() {
        return qsReturnBankName;
    }

    /**
     * Sets the settling bank name.
     *
     * @param v settling bank name
     */
    public void setQsReturnBankName(final String v) {
        this.qsReturnBankName = v;
    }

    /**
     * Returns the bank-side return code (清算指令银行返回码, 1-2 digits).
     *
     * @return bank return code
     */
    public String getQsReturnCode() {
        return qsReturnCode;
    }

    /**
     * Sets the bank-side return code.
     *
     * @param v bank return code
     */
    public void setQsReturnCode(final String v) {
        this.qsReturnCode = v;
    }

    /**
     * Returns the bank-side business serial number (银行对应业务流水号, 0-100 chars).
     *
     * @return bank business serial number
     */
    public String getQsReturnSerialNo() {
        return qsReturnSerialNo;
    }

    /**
     * Sets the bank-side business serial number.
     *
     * @param v bank business serial number
     */
    public void setQsReturnSerialNo(final String v) {
        this.qsReturnSerialNo = v;
    }

    /**
     * Returns the bank transaction date (银行交易日期, yyyyMMdd).
     *
     * @return bank transaction date
     */
    public String getQsReturnDate() {
        return qsReturnDate;
    }

    /**
     * Sets the bank transaction date.
     *
     * @param v bank transaction date (yyyyMMdd)
     */
    public void setQsReturnDate(final String v) {
        this.qsReturnDate = v;
    }

    /**
     * Returns the optional bank-side receipt file name (银行对应业务回单文件名).
     *
     * @return bank receipt file name, or {@code null} if absent
     */
    public String getQsReturnFilename() {
        return qsReturnFilename;
    }

    /**
     * Sets the optional bank-side receipt file name.
     *
     * @param v bank receipt file name
     */
    public void setQsReturnFilename(final String v) {
        this.qsReturnFilename = v;
    }

    /**
     * Returns the optional bank return remarks (清算指令银行返回说明, 0-200 chars).
     *
     * @return bank return memo, or {@code null} if absent
     */
    public String getQsReturnMemo() {
        return qsReturnMemo;
    }

    /**
     * Sets the optional bank return remarks.
     *
     * @param v bank return memo
     */
    public void setQsReturnMemo(final String v) {
        this.qsReturnMemo = v;
    }
}
