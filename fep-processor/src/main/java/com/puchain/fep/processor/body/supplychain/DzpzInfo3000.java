package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3000 电子凭证信息报文业务体 (PRD §3.2)。
 *
 * <p>字段顺序严格对应 {@code 3000.xsd} 中 {@code dzpzInfo3000} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, ApplyMode, pzInfo?, ExtInfo?。</p>
 *
 * <p>顶层 4 个标量字段（SerialNo / SendNodeCode / DesNodeCode / ApplyMode）必填；
 * {@link PzInfo} 与 {@link ExtInfo} 为可选嵌套块（{@code minOccurs="0"}）。
 * 所有标量字段 Java 类型统一为 {@link String}；XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DzpzInfo3000")
@XmlType(name = "DzpzInfo3000", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode", "applyMode",
        "pzInfo", "extInfo"
})
public class DzpzInfo3000 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "ApplyMode", required = true)
    private String applyMode;

    @XmlElement(name = "pzInfo")
    private PzInfo pzInfo;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    /**
     * Returns the serial number (流水号).
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
     * Returns the certificate registration business mode (凭证登记业务分类).
     *
     * @return apply mode
     */
    public String getApplyMode() {
        return applyMode;
    }

    /**
     * Sets the certificate registration business mode.
     *
     * @param v apply mode
     */
    public void setApplyMode(final String v) {
        this.applyMode = v;
    }

    /**
     * Returns the optional certificate detail block (电子凭证信息).
     *
     * @return pz info, or {@code null} if absent
     */
    public PzInfo getPzInfo() {
        return pzInfo;
    }

    /**
     * Sets the optional certificate detail block.
     *
     * @param v pz info
     */
    public void setPzInfo(final PzInfo v) {
        this.pzInfo = v;
    }

    /**
     * Returns the optional extension info block (自定义扩展信息).
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
