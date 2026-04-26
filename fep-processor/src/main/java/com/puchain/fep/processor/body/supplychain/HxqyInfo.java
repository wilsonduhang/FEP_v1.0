package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code hxqyInfo} — 核心企业基本信息（2 字段）。
 *
 * <p>字段顺序严格对应 {@code 3107.xsd} 中 {@code hxqyInfo} complexType 的 sequence：
 * hxqyName, hxqyCode。两字段均为 required（XSD 无 {@code minOccurs="0"}）。</p>
 *
 * <p><b>跨报文复用</b>：本类首建于 T4，{@code 3107.xsd} 与 {@code 3112.xsd} 均引用同名
 * {@code hxqyInfo} complexType 且字段集合 100% 一致（diff 仅 documentation 注释差异），
 * 因此 T6 (3112) 直接复用本类，无需新建。{@code 3107.xsd} 中作为 {@code maxOccurs="200"}
 * 的列表元素出现在 {@link PzCheckQuery3107#getHxqyInfo()}。</p>
 *
 * <p><b>区别于 {@code HxqyInfo3109}</b>：T5 的 {@code HxqyInfo3109} 是 3109.xsd 内同名
 * 但字段集合完全不同的 complexType（3109 是 hxqyNum + hxqyInfoMx wrapper；本类是
 * hxqyName + hxqyCode），两者属于独立 POJO，命名不带数字后缀以匹配本 complexType
 * 在 3107/3112 间共享的事实；T5 类带 {@code 3109} 后缀以避免歧义。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hxqyInfo")
@XmlType(name = "hxqyInfo", propOrder = {
        "hxqyName", "hxqyCode"
})
public class HxqyInfo extends CfxBody {

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

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
}
