package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 3115 平台资金清算指令及回执报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3115.xsd} 中 {@code PlatPay3115} complexType 的 sequence
 * (11 字段): SerialNo, SendNodeCode, DesNodeCode, PlatPayNo, hxqyName?,
 * hxqyCode?, qsInfo[1..200], SignElement?, qsfqSign?, PlatSign?, ExtInfo?。
 * 4 字段 required（SerialNo, SendNodeCode, DesNodeCode, PlatPayNo）+ qsInfo
 * required 列表；6 字段 optional（{@code minOccurs="0"}）。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link QsInfo}{@code >} — 清算指令信息列表 ({@code maxOccurs="200"})；
 *       每条 {@link QsInfo} 内可挂可选 {@link QsReturnInfo} 银行回执块。</li>
 *   <li>{@link ExtInfo} — DataType.xsd 共享扩展块（body.common P2c 已落地）</li>
 * </ul></p>
 *
 * <p><b>双向报文</b>：3115 是请求-应答合一的双向 complexType（业务头 {@code BatchHead3115}
 * 类型 {@code RequestResponseHead}），同一 POJO 既承载发起方下发的清算指令，也承载
 * 银行回填回执后的回执版本（通过 {@link QsInfo#getQsReturnInfo()} 区分）。无独立
 * {@code PlatPay3115Return} 类。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PlatPay3115")
@XmlType(name = "PlatPay3115", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "platPayNo", "hxqyName", "hxqyCode",
        "qsInfo",
        "signElement", "qsfqSign", "platSign",
        "extInfo"
})
public class PlatPay3115 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "PlatPayNo", required = true)
    private String platPayNo;

    @XmlElement(name = "hxqyName")
    private String hxqyName;

    @XmlElement(name = "hxqyCode")
    private String hxqyCode;

    @XmlElement(name = "qsInfo", required = true)
    private List<QsInfo> qsInfo;

    @XmlElement(name = "SignElement")
    private String signElement;

    @XmlElement(name = "qsfqSign")
    private String qsfqSign;

    @XmlElement(name = "PlatSign")
    private String platSign;

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
     * Returns the sending node code (发送方节点代码, 14-char).
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
     * Returns the destination node code (接收方节点代码, 14-char).
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
     * Returns the platform-side settlement instruction number (清算指令平台编号, 10-30 chars).
     *
     * @return platform settlement instruction number
     */
    public String getPlatPayNo() {
        return platPayNo;
    }

    /**
     * Sets the platform-side settlement instruction number.
     *
     * @param v platform settlement instruction number
     */
    public void setPlatPayNo(final String v) {
        this.platPayNo = v;
    }

    /**
     * Returns the optional core enterprise name (核心企业名称).
     *
     * @return core enterprise name, or {@code null} if absent
     */
    public String getHxqyName() {
        return hxqyName;
    }

    /**
     * Sets the optional core enterprise name.
     *
     * @param v core enterprise name
     */
    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    /**
     * Returns the optional core enterprise unified social credit code (核心企业 USCI).
     *
     * @return core enterprise USCI, or {@code null} if absent
     */
    public String getHxqyCode() {
        return hxqyCode;
    }

    /**
     * Sets the optional core enterprise unified social credit code.
     *
     * @param v core enterprise USCI
     */
    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    /**
     * Returns the settlement instruction list (清算指令信息列表, 1-200 entries).
     *
     * @return settlement instruction list
     */
    public List<QsInfo> getQsInfo() {
        return qsInfo;
    }

    /**
     * Sets the settlement instruction list.
     *
     * @param v settlement instruction list
     */
    public void setQsInfo(final List<QsInfo> v) {
        this.qsInfo = v;
    }

    /**
     * Returns the optional signature element descriptor (指令发起方清算指令签名要素,
     * default {@code "fkrAccName|fkrAccNo|skrAccName|skrAccNo|Amt|WishDate"}).
     *
     * @return signature element descriptor, or {@code null} if absent
     */
    public String getSignElement() {
        return signElement;
    }

    /**
     * Sets the optional signature element descriptor.
     *
     * @param v signature element descriptor
     */
    public void setSignElement(final String v) {
        this.signElement = v;
    }

    /**
     * Returns the optional initiator-side PK7 settlement signature (指令发起方清算指令签名).
     *
     * @return initiator settlement signature, or {@code null} if absent
     */
    public String getQsfqSign() {
        return qsfqSign;
    }

    /**
     * Sets the optional initiator-side PK7 settlement signature.
     *
     * @param v initiator settlement signature
     */
    public void setQsfqSign(final String v) {
        this.qsfqSign = v;
    }

    /**
     * Returns the optional platform-side PK7 settlement signature (平台方清算指令签名).
     *
     * @return platform settlement signature, or {@code null} if absent
     */
    public String getPlatSign() {
        return platSign;
    }

    /**
     * Sets the optional platform-side PK7 settlement signature.
     *
     * @param v platform settlement signature
     */
    public void setPlatSign(final String v) {
        this.platSign = v;
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
