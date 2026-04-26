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
 * 3107 平台凭证对账申请报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3107.xsd} 中 {@code pzCheckQuery3107} complexType 的 sequence
 * (7 字段): SerialNo, SendNodeCode, DesNodeCode, CheckDate, hxqyNum,
 * hxqyInfo[1..200], ExtInfo?。前 5 字段 + hxqyInfo 列表为 required；ExtInfo optional。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link HxqyInfo}{@code >} — 核心企业信息列表（{@code maxOccurs="200"}），
 *       本类首建 {@link HxqyInfo}，T6 3112 复用</li>
 *   <li>{@link ExtInfo} — DataType.xsd 共享扩展块（body.common P2c 已落地）</li>
 * </ul></p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 层的长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzCheckQuery3107")
@XmlType(name = "pzCheckQuery3107", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "checkDate", "hxqyNum",
        "hxqyInfo",
        "extInfo"
})
public class PzCheckQuery3107 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "CheckDate", required = true)
    private String checkDate;

    @XmlElement(name = "hxqyNum", required = true)
    private String hxqyNum;

    @XmlElement(name = "hxqyInfo", required = true)
    private List<HxqyInfo> hxqyInfo;

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
     * Returns the reconciliation date (对账日期, yyyyMMdd).
     *
     * @return reconciliation date
     */
    public String getCheckDate() {
        return checkDate;
    }

    /**
     * Sets the reconciliation date.
     *
     * @param v reconciliation date (yyyyMMdd)
     */
    public void setCheckDate(final String v) {
        this.checkDate = v;
    }

    /**
     * Returns the count of core enterprises (核心企业数量, 1-2 digits).
     *
     * @return core enterprise count
     */
    public String getHxqyNum() {
        return hxqyNum;
    }

    /**
     * Sets the count of core enterprises.
     *
     * @param v core enterprise count
     */
    public void setHxqyNum(final String v) {
        this.hxqyNum = v;
    }

    /**
     * Returns the core enterprise info list (核心企业信息列表, 1-200 entries).
     *
     * @return core enterprise info list
     */
    public List<HxqyInfo> getHxqyInfo() {
        return hxqyInfo;
    }

    /**
     * Sets the core enterprise info list.
     *
     * @param v core enterprise info list
     */
    public void setHxqyInfo(final List<HxqyInfo> v) {
        this.hxqyInfo = v;
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
