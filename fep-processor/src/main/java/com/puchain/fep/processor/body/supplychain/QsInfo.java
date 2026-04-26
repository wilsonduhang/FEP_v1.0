package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code qsInfo} — 清算指令信息（12 字段）。
 *
 * <p>字段顺序严格对应 {@code 3115.xsd} 中 {@code qsInfo} complexType 的 sequence：
 * qsSerialNo, pzNo?, fkfAccName, fkfAccNo, skfAccName, skfAccNo,
 * skfBankCode?, Amt, WishDate, qsPostscript?, qsMemo?, qsReturnInfo?。
 * 7 字段 required（qsSerialNo, fkfAccName, fkfAccNo, skfAccName, skfAccNo,
 * Amt, WishDate）；5 字段 optional（{@code minOccurs="0"}）。</p>
 *
 * <p>嵌入在 {@link PlatPay3115#getQsInfo()}（{@code maxOccurs="200"}）下，
 * 单条对应一笔清算指令。可选 {@link QsReturnInfo} 子元素承载银行回执。
 * 仅 3115 报文使用本类型。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qsInfo")
@XmlType(name = "qsInfo", propOrder = {
        "qsSerialNo", "pzNo",
        "fkfAccName", "fkfAccNo",
        "skfAccName", "skfAccNo", "skfBankCode",
        "amt", "wishDate",
        "qsPostscript", "qsMemo",
        "qsReturnInfo"
})
public class QsInfo extends CfxBody {

    @XmlElement(name = "qsSerialNo", required = true)
    private String qsSerialNo;

    @XmlElement(name = "pzNo")
    private String pzNo;

    @XmlElement(name = "fkfAccName", required = true)
    private String fkfAccName;

    @XmlElement(name = "fkfAccNo", required = true)
    private String fkfAccNo;

    @XmlElement(name = "skfAccName", required = true)
    private String skfAccName;

    @XmlElement(name = "skfAccNo", required = true)
    private String skfAccNo;

    @XmlElement(name = "skfBankCode")
    private String skfBankCode;

    @XmlElement(name = "Amt", required = true)
    private String amt;

    @XmlElement(name = "WishDate", required = true)
    private String wishDate;

    @XmlElement(name = "qsPostscript")
    private String qsPostscript;

    @XmlElement(name = "qsMemo")
    private String qsMemo;

    @XmlElement(name = "qsReturnInfo")
    private QsReturnInfo qsReturnInfo;

    /**
     * Returns the settlement-instruction serial number (清算指令流水号).
     *
     * @return settlement serial number
     */
    public String getQsSerialNo() {
        return qsSerialNo;
    }

    /**
     * Sets the settlement-instruction serial number.
     *
     * @param v settlement serial number
     */
    public void setQsSerialNo(final String v) {
        this.qsSerialNo = v;
    }

    /**
     * Returns the optional related electronic voucher number (关联电子凭证编号).
     *
     * @return voucher number, or {@code null} if absent
     */
    public String getPzNo() {
        return pzNo;
    }

    /**
     * Sets the optional related electronic voucher number.
     *
     * @param v voucher number
     */
    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    /**
     * Returns the payer account name (付款方账户名称).
     *
     * @return payer account name
     */
    public String getFkfAccName() {
        return fkfAccName;
    }

    /**
     * Sets the payer account name.
     *
     * @param v payer account name
     */
    public void setFkfAccName(final String v) {
        this.fkfAccName = v;
    }

    /**
     * Returns the payer account number (付款方账号).
     *
     * @return payer account number
     */
    public String getFkfAccNo() {
        return fkfAccNo;
    }

    /**
     * Sets the payer account number.
     *
     * @param v payer account number
     */
    public void setFkfAccNo(final String v) {
        this.fkfAccNo = v;
    }

    /**
     * Returns the payee account name (收款方账户名称).
     *
     * @return payee account name
     */
    public String getSkfAccName() {
        return skfAccName;
    }

    /**
     * Sets the payee account name.
     *
     * @param v payee account name
     */
    public void setSkfAccName(final String v) {
        this.skfAccName = v;
    }

    /**
     * Returns the payee account number (收款方账号).
     *
     * @return payee account number
     */
    public String getSkfAccNo() {
        return skfAccNo;
    }

    /**
     * Sets the payee account number.
     *
     * @param v payee account number
     */
    public void setSkfAccNo(final String v) {
        this.skfAccNo = v;
    }

    /**
     * Returns the optional payee bank settlement code (收款方银行清算行号).
     *
     * @return payee bank code, or {@code null} if absent
     */
    public String getSkfBankCode() {
        return skfBankCode;
    }

    /**
     * Sets the optional payee bank settlement code.
     *
     * @param v payee bank code
     */
    public void setSkfBankCode(final String v) {
        this.skfBankCode = v;
    }

    /**
     * Returns the payment amount (支付金额, two-decimal currency string).
     *
     * @return payment amount
     */
    public String getAmt() {
        return amt;
    }

    /**
     * Sets the payment amount.
     *
     * @param v payment amount (two-decimal currency string)
     */
    public void setAmt(final String v) {
        this.amt = v;
    }

    /**
     * Returns the desired settlement date (期望日, yyyyMMdd).
     *
     * @return desired settlement date
     */
    public String getWishDate() {
        return wishDate;
    }

    /**
     * Sets the desired settlement date.
     *
     * @param v desired settlement date (yyyyMMdd)
     */
    public void setWishDate(final String v) {
        this.wishDate = v;
    }

    /**
     * Returns the optional settlement-instruction postscript (清算指令附言, 0-200 chars).
     *
     * @return postscript, or {@code null} if absent
     */
    public String getQsPostscript() {
        return qsPostscript;
    }

    /**
     * Sets the optional settlement-instruction postscript.
     *
     * @param v postscript
     */
    public void setQsPostscript(final String v) {
        this.qsPostscript = v;
    }

    /**
     * Returns the optional settlement-instruction memo (清算指令备注, 0-200 chars).
     *
     * @return memo, or {@code null} if absent
     */
    public String getQsMemo() {
        return qsMemo;
    }

    /**
     * Sets the optional settlement-instruction memo.
     *
     * @param v memo
     */
    public void setQsMemo(final String v) {
        this.qsMemo = v;
    }

    /**
     * Returns the optional bank settlement receipt block (清算回执信息).
     *
     * @return settlement receipt info, or {@code null} if absent
     */
    public QsReturnInfo getQsReturnInfo() {
        return qsReturnInfo;
    }

    /**
     * Sets the optional bank settlement receipt block.
     *
     * @param v settlement receipt info
     */
    public void setQsReturnInfo(final QsReturnInfo v) {
        this.qsReturnInfo = v;
    }
}
