package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code CreditInfoBank} — 核心企业银行授信明细（5 字段）。
 *
 * <p>字段顺序严格对应 {@code 3113.xsd} 中 {@code CreditInfoBank} complexType 的 sequence：
 * BankNodeCode, BankName, sxAmt, sxBalance, QueryReturnTime。全部 5 字段 required
 * （XSD 无 {@code minOccurs="0"}）。</p>
 *
 * <p>该 complexType 仅出现在 3113 报文的
 * {@link CreditInfo#getCreditInfoMx()} 字段（{@code minOccurs="0"} / {@code maxOccurs="50"}
 * 列表），scope 局限于 supplychain 包，不复用为 body.common。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CreditInfoBank")
@XmlType(name = "CreditInfoBank", propOrder = {
        "bankNodeCode", "bankName",
        "sxAmt", "sxBalance",
        "queryReturnTime"
})
public class CreditInfoBank extends CfxBody {

    @XmlElement(name = "BankNodeCode", required = true)
    private String bankNodeCode;

    @XmlElement(name = "BankName", required = true)
    private String bankName;

    @XmlElement(name = "sxAmt", required = true)
    private String sxAmt;

    @XmlElement(name = "sxBalance", required = true)
    private String sxBalance;

    @XmlElement(name = "QueryReturnTime", required = true)
    private String queryReturnTime;

    /**
     * Returns the bank node code (银行代码, 14-char).
     *
     * @return bank node code
     */
    public String getBankNodeCode() {
        return bankNodeCode;
    }

    /**
     * Sets the bank node code.
     *
     * @param v bank node code
     */
    public void setBankNodeCode(final String v) {
        this.bankNodeCode = v;
    }

    /**
     * Returns the bank name (银行名称).
     *
     * @return bank name
     */
    public String getBankName() {
        return bankName;
    }

    /**
     * Sets the bank name.
     *
     * @param v bank name
     */
    public void setBankName(final String v) {
        this.bankName = v;
    }

    /**
     * Returns the total credit amount granted to the core enterprise (核心企业授信总额).
     *
     * @return total credit amount
     */
    public String getSxAmt() {
        return sxAmt;
    }

    /**
     * Sets the total credit amount granted to the core enterprise.
     *
     * @param v total credit amount
     */
    public void setSxAmt(final String v) {
        this.sxAmt = v;
    }

    /**
     * Returns the remaining credit balance for the core enterprise (核心企业授信余额).
     *
     * @return credit balance
     */
    public String getSxBalance() {
        return sxBalance;
    }

    /**
     * Sets the remaining credit balance for the core enterprise.
     *
     * @param v credit balance
     */
    public void setSxBalance(final String v) {
        this.sxBalance = v;
    }

    /**
     * Returns the bank's query return timestamp (银行查询返回时间, yyyyMMddHHmmss).
     *
     * @return query return time
     */
    public String getQueryReturnTime() {
        return queryReturnTime;
    }

    /**
     * Sets the bank's query return timestamp.
     *
     * @param v query return time (yyyyMMddHHmmss)
     */
    public void setQueryReturnTime(final String v) {
        this.queryReturnTime = v;
    }
}
