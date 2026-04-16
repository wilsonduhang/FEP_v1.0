package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import com.puchain.fep.processor.body.common.PzrzStatusInfo;
import com.puchain.fep.processor.body.common.RiskRate;
import com.puchain.fep.processor.body.common.ZpzAllInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 3004 电子凭证融资状态查询回执报文业务体 — 最复杂的供应链报文类型。
 *
 * <p>字段顺序严格对应 {@code 3004.xsd} 中 {@code pzInfoReturn3004} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, hxqyName, hxqyCode, pzNo, pzState, pzrzState,
 * pzrzStatusInfo, RiskRate* (maxOccurs=10), edUpdateDateTime?, pzInfo?,
 * zpzAllInfo* (maxOccurs=200), ExtInfo?。</p>
 *
 * <p>包含两个 {@link List} 类型嵌套字段：
 * <ul>
 *   <li>{@code List<RiskRate>} — 风险费率列表，最多 10 项</li>
 *   <li>{@code List<ZpzAllInfo>} — 子凭证汇总信息列表，最多 200 项</li>
 * </ul>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；复杂类型使用对应 POJO 引用。
 * XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pzInfoReturn3004")
@XmlType(propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "hxqyName", "hxqyCode", "pzNo", "pzState", "pzrzState",
        "pzrzStatusInfo", "riskRateList", "edUpdateDateTime",
        "pzInfo", "zpzAllInfoList", "extInfo"
})
public class PzInfoReturn3004 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "pzNo", required = true)
    private String pzNo;

    @XmlElement(name = "pzState", required = true)
    private String pzState;

    @XmlElement(name = "pzrzState", required = true)
    private String pzrzState;

    @XmlElement(name = "pzrzStatusInfo", required = true)
    private PzrzStatusInfo pzrzStatusInfo;

    @XmlElement(name = "RiskRate")
    private List<RiskRate> riskRateList;

    @XmlElement(name = "edUpdateDateTime")
    private String edUpdateDateTime;

    @XmlElement(name = "pzInfo")
    private PzInfo pzInfo;

    @XmlElement(name = "zpzAllInfo")
    private List<ZpzAllInfo> zpzAllInfoList;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    /**
     * Returns the serial number.
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
     * Returns the core enterprise name.
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
     * Returns the core enterprise USCI code (18-char).
     *
     * @return core enterprise USCI code
     */
    public String getHxqyCode() {
        return hxqyCode;
    }

    /**
     * Sets the core enterprise USCI code.
     *
     * @param v core enterprise USCI code
     */
    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    /**
     * Returns the certificate number (凭证编号).
     *
     * @return certificate number
     */
    public String getPzNo() {
        return pzNo;
    }

    /**
     * Sets the certificate number.
     *
     * @param v certificate number
     */
    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    /**
     * Returns the certificate state code.
     *
     * @return certificate state
     */
    public String getPzState() {
        return pzState;
    }

    /**
     * Sets the certificate state code.
     *
     * @param v certificate state
     */
    public void setPzState(final String v) {
        this.pzState = v;
    }

    /**
     * Returns the certificate financing state code.
     *
     * @return certificate financing state
     */
    public String getPzrzState() {
        return pzrzState;
    }

    /**
     * Sets the certificate financing state code.
     *
     * @param v certificate financing state
     */
    public void setPzrzState(final String v) {
        this.pzrzState = v;
    }

    /**
     * Returns the certificate financing status info (required nested type).
     *
     * @return certificate financing status info
     */
    public PzrzStatusInfo getPzrzStatusInfo() {
        return pzrzStatusInfo;
    }

    /**
     * Sets the certificate financing status info.
     *
     * @param v certificate financing status info
     */
    public void setPzrzStatusInfo(final PzrzStatusInfo v) {
        this.pzrzStatusInfo = v;
    }

    /**
     * Returns the risk rate list (maxOccurs=10), or {@code null} if absent.
     *
     * @return risk rate list, may be {@code null}
     */
    public List<RiskRate> getRiskRateList() {
        return riskRateList;
    }

    /**
     * Sets the risk rate list.
     *
     * @param v risk rate list
     */
    public void setRiskRateList(final List<RiskRate> v) {
        this.riskRateList = v;
    }

    /**
     * Returns the optional electronic document update date-time.
     *
     * @return update date-time string, or {@code null} if absent
     */
    public String getEdUpdateDateTime() {
        return edUpdateDateTime;
    }

    /**
     * Sets the electronic document update date-time.
     *
     * @param v update date-time string
     */
    public void setEdUpdateDateTime(final String v) {
        this.edUpdateDateTime = v;
    }

    /**
     * Returns the optional certificate detail info.
     *
     * @return certificate detail, or {@code null} if absent
     */
    public PzInfo getPzInfo() {
        return pzInfo;
    }

    /**
     * Sets the certificate detail info.
     *
     * @param v certificate detail
     */
    public void setPzInfo(final PzInfo v) {
        this.pzInfo = v;
    }

    /**
     * Returns the sub-certificate summary list (maxOccurs=200), or {@code null} if absent.
     *
     * @return sub-certificate summary list, may be {@code null}
     */
    public List<ZpzAllInfo> getZpzAllInfoList() {
        return zpzAllInfoList;
    }

    /**
     * Sets the sub-certificate summary list.
     *
     * @param v sub-certificate summary list
     */
    public void setZpzAllInfoList(final List<ZpzAllInfo> v) {
        this.zpzAllInfoList = v;
    }

    /**
     * Returns the optional extension info block.
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
