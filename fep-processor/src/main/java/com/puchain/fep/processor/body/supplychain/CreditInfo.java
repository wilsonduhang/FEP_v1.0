package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * Local complexType {@code CreditInfo} — 核心企业银行授信查询回执（5 字段）。
 *
 * <p>字段顺序严格对应 {@code 3113.xsd} 中 {@code CreditInfo} complexType 的 sequence：
 * hxqyName, hxqyCode, RetCode, RetMemo?, CreditInfoMx[0..50]?。3 字段 required +
 * 2 字段 optional ({@code RetMemo} 和 {@code CreditInfoMx} 列表均为
 * {@code minOccurs="0"})。</p>
 *
 * <p>该 complexType 仅出现在 3113 报文的
 * {@link HxqyCreditAmt3113#getCreditInfo()} 字段（{@code maxOccurs="200"} 列表），
 * scope 局限于 supplychain 包，不复用为 body.common。</p>
 *
 * <p><b>嵌套类型</b>：{@code List<}{@link CreditInfoBank}{@code >} —
 * 核心企业银行授信明细列表 ({@code minOccurs="0"} / {@code maxOccurs="50"})。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CreditInfo")
@XmlType(name = "CreditInfo", propOrder = {
        "hxqyName", "hxqyCode",
        "retCode", "retMemo",
        "creditInfoMx"
})
public class CreditInfo extends CfxBody {

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "RetCode", required = true)
    private String retCode;

    @XmlElement(name = "RetMemo")
    private String retMemo;

    @XmlElement(name = "CreditInfoMx")
    private List<CreditInfoBank> creditInfoMx;

    /**
     * Returns the core enterprise name (核心企业名称).
     *
     * @return core enterprise name
     */
    public String getHxqyName() {
        return hxqyName;
    }

    /**
     * Sets the core enterprise name.
     *
     * @param v core enterprise name
     */
    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    /**
     * Returns the core enterprise unified social credit code (核心企业统一社会信用代码, 18-char USCI).
     *
     * @return core enterprise USCI
     */
    public String getHxqyCode() {
        return hxqyCode;
    }

    /**
     * Sets the core enterprise unified social credit code.
     *
     * @param v core enterprise USCI
     */
    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    /**
     * Returns the credit query result code (核心企业授信查询结果码).
     *
     * @return query result code
     */
    public String getRetCode() {
        return retCode;
    }

    /**
     * Sets the credit query result code.
     *
     * @param v query result code
     */
    public void setRetCode(final String v) {
        this.retCode = v;
    }

    /**
     * Returns the optional credit query result memo (核心企业授信查询结果码备注, optional, ≤ 100 chars).
     *
     * @return query result memo, or {@code null} if absent
     */
    public String getRetMemo() {
        return retMemo;
    }

    /**
     * Sets the optional credit query result memo.
     *
     * @param v query result memo
     */
    public void setRetMemo(final String v) {
        this.retMemo = v;
    }

    /**
     * Returns the optional bank credit detail list
     * (核心企业银行授信明细列表, optional, 0-50 entries).
     *
     * @return bank credit detail list, or {@code null} if absent
     */
    public List<CreditInfoBank> getCreditInfoMx() {
        return creditInfoMx;
    }

    /**
     * Sets the optional bank credit detail list.
     *
     * @param v bank credit detail list
     */
    public void setCreditInfoMx(final List<CreditInfoBank> v) {
        this.creditInfoMx = v;
    }
}
