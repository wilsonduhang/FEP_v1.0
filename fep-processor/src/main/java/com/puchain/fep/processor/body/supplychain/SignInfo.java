package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3105 核心企业 / 融资企业 / 平台申请签名信息段 ({@code SignInfo}, local complexType in 3105.xsd).
 *
 * <p>Field order matches the XSD sequence (4 fields, 1 required + 3 optional):
 * SignElement, hxqySign?, rzqySign?, PlatSign?。{@code SignElement} 在 XSD 中带
 * default value {@code "hxqyName|rzqyName|rzAmt|ApplyDate|EndDate"}，但 JAXB POJO
 * 不在 Java 层重复 default — 由 XSD validation / 构造调用方负责填充。</p>
 *
 * <p>Used as nested element of {@link RzApplyInfo3105}; only relevant within 3105
 * 融资申请 message context. {@code hxqySign / rzqySign / PlatSign} 字段类型为
 * {@code PK7Sign}（XSD 字符串约束），Java 层统一为 {@link String}；XSD 长度/格式约束
 * 由 {@link com.puchain.fep.processor.validation.XsdValidator} 强制。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SignInfo")
@XmlType(propOrder = {"signElement", "hxqySign", "rzqySign", "platSign"})
public class SignInfo extends CfxBody {

    @XmlElement(name = "SignElement", required = true)
    private String signElement;

    @XmlElement(name = "hxqySign")
    private String hxqySign;

    @XmlElement(name = "rzqySign")
    private String rzqySign;

    @XmlElement(name = "PlatSign")
    private String platSign;

    public String getSignElement() {
        return signElement;
    }

    public void setSignElement(final String v) {
        this.signElement = v;
    }

    public String getHxqySign() {
        return hxqySign;
    }

    public void setHxqySign(final String v) {
        this.hxqySign = v;
    }

    public String getRzqySign() {
        return rzqySign;
    }

    public void setRzqySign(final String v) {
        this.rzqySign = v;
    }

    public String getPlatSign() {
        return platSign;
    }

    public void setPlatSign(final String v) {
        this.platSign = v;
    }
}
