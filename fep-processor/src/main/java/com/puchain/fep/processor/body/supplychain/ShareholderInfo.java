package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code ShareholderInfo} — 平台企业主要股东信息（2 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code ShareholderInfo} complexType 的 sequence:
 * ShareholderName, ShareProportion。两字段均为 required。</p>
 *
 * <p>本类作为 {@link PlatInfo#getShareholderInfo()} 的列表元素出现，
 * {@code maxOccurs="10"} 限制每条平台登记最多 10 个主要股东。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ShareholderInfo")
@XmlType(name = "ShareholderInfo", propOrder = {
        "shareholderName", "shareProportion"
})
public class ShareholderInfo extends CfxBody {

    @XmlElement(name = "ShareholderName", required = true)
    private String shareholderName;

    @XmlElement(name = "ShareProportion", required = true)
    private String shareProportion;

    /**
     * Returns the shareholder name (股东名称).
     *
     * @return shareholder name
     */
    public String getShareholderName() {
        return shareholderName;
    }

    /**
     * Sets the shareholder name.
     *
     * @param v shareholder name
     */
    public void setShareholderName(final String v) {
        this.shareholderName = v;
    }

    /**
     * Returns the shareholding proportion (持股比例).
     *
     * @return shareholding proportion
     */
    public String getShareProportion() {
        return shareProportion;
    }

    /**
     * Sets the shareholding proportion.
     *
     * @param v shareholding proportion
     */
    public void setShareProportion(final String v) {
        this.shareProportion = v;
    }
}
