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
 * 3116 银行每日资金对账报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3116.xsd} 中 {@code BankCheckDay3116} complexType 的 sequence
 * (9 字段): SerialNo, SendNodeCode, DesNodeCode, hxqyName, hxqyCode, CheckDate,
 * CheckDetailNum, CheckDetailInfo[1..200], ExtInfo?。前 7 字段 + CheckDetailInfo
 * 列表为 required；ExtInfo optional ({@code minOccurs="0"})。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link CheckDetailInfo}{@code >} — 当日交易明细列表
 *       ({@code maxOccurs="200"})；单条对应一笔银行资金当日发生额。</li>
 *   <li>{@link ExtInfo} — DataType.xsd 共享扩展块（body.common P2c 已落地）</li>
 * </ul></p>
 *
 * <p><b>单向报文</b>：3116 是银行 → 平台单向上送（业务头 {@code BatchHead3116}
 * 类型 {@code RequestHead}），无独立回执报文。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BankCheckDay3116")
@XmlType(name = "BankCheckDay3116", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "hxqyName", "hxqyCode",
        "checkDate", "checkDetailNum",
        "checkDetailInfo",
        "extInfo"
})
public class BankCheckDay3116 extends CfxBody {

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

    @XmlElement(name = "CheckDate", required = true)
    private String checkDate;

    @XmlElement(name = "CheckDetailNum", required = true)
    private String checkDetailNum;

    @XmlElement(name = "CheckDetailInfo", required = true)
    private List<CheckDetailInfo> checkDetailInfo;

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
     * Returns the core enterprise unified social credit code (核心企业 USCI, 18-char).
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
     * Returns the count of detail rows (交易明细条数).
     *
     * @return detail count
     */
    public String getCheckDetailNum() {
        return checkDetailNum;
    }

    /**
     * Sets the count of detail rows.
     *
     * @param v detail count
     */
    public void setCheckDetailNum(final String v) {
        this.checkDetailNum = v;
    }

    /**
     * Returns the daily transaction detail list (当日交易明细, 1-200 entries).
     *
     * @return transaction detail list
     */
    public List<CheckDetailInfo> getCheckDetailInfo() {
        return checkDetailInfo;
    }

    /**
     * Sets the daily transaction detail list.
     *
     * @param v transaction detail list
     */
    public void setCheckDetailInfo(final List<CheckDetailInfo> v) {
        this.checkDetailInfo = v;
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
