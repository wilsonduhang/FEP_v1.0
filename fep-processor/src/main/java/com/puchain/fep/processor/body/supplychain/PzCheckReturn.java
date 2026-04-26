package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code pzCheckReturn} — 平台凭证核对回执列表项（7 字段）。
 *
 * <p>字段顺序严格对应 {@code 3108.xsd} 中 {@code pzCheckReturn} complexType 的 sequence：
 * hxqyName, hxqyCode, RetCode, RetMemo?, pzCountAll, pzAmtAll, pzFilename?。
 * 5 字段 required + 2 字段 optional ({@code RetMemo} / {@code pzFilename})。</p>
 *
 * <p>该 complexType 仅出现在 3108 报文的
 * {@link PzCheckQueryReturn3108#getPzCheckReturn()} 字段（{@code maxOccurs="200"}
 * 列表），scope 局限于 supplychain 包，不复用为 body.common。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzCheckReturn")
@XmlType(name = "pzCheckReturn", propOrder = {
        "hxqyName", "hxqyCode",
        "retCode", "retMemo",
        "pzCountAll", "pzAmtAll",
        "pzFilename"
})
public class PzCheckReturn extends CfxBody {

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "RetCode", required = true)
    private String retCode;

    @XmlElement(name = "RetMemo")
    private String retMemo;

    @XmlElement(name = "pzCountAll", required = true)
    private String pzCountAll;

    @XmlElement(name = "pzAmtAll", required = true)
    private String pzAmtAll;

    @XmlElement(name = "pzFilename")
    private String pzFilename;

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
     * Returns the query result code (查询结果码).
     *
     * @return query result code
     */
    public String getRetCode() {
        return retCode;
    }

    /**
     * Sets the query result code.
     *
     * @param v query result code
     */
    public void setRetCode(final String v) {
        this.retCode = v;
    }

    /**
     * Returns the optional query result memo (查询结果码备注, optional, ≤ 100 chars).
     *
     * @return query result memo, or {@code null} if absent
     */
    public String getRetMemo() {
        return retMemo;
    }

    /**
     * Sets the optional query result memo.
     *
     * @param v query result memo
     */
    public void setRetMemo(final String v) {
        this.retMemo = v;
    }

    /**
     * Returns the total count of valid vouchers (有效凭证总数).
     *
     * @return total valid voucher count
     */
    public String getPzCountAll() {
        return pzCountAll;
    }

    /**
     * Sets the total count of valid vouchers.
     *
     * @param v total valid voucher count
     */
    public void setPzCountAll(final String v) {
        this.pzCountAll = v;
    }

    /**
     * Returns the total amount of valid vouchers (有效凭证总金额).
     *
     * @return total valid voucher amount
     */
    public String getPzAmtAll() {
        return pzAmtAll;
    }

    /**
     * Sets the total amount of valid vouchers.
     *
     * @param v total valid voucher amount
     */
    public void setPzAmtAll(final String v) {
        this.pzAmtAll = v;
    }

    /**
     * Returns the optional voucher filename (凭证文件名, optional).
     *
     * @return voucher filename, or {@code null} if absent
     */
    public String getPzFilename() {
        return pzFilename;
    }

    /**
     * Sets the optional voucher filename.
     *
     * @param v voucher filename
     */
    public void setPzFilename(final String v) {
        this.pzFilename = v;
    }
}
