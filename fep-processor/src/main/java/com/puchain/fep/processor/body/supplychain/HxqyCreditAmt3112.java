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
 * 3112 核心企业授信余额查询报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3112.xsd} 中 {@code hxqyCreditAmt3112} complexType 的 sequence
 * (7 字段): SerialNo, SendNodeCode, DesNodeCode, QueryDate, hxqyInfoNum,
 * hxqyInfo[1..200], ExtInfo?。前 5 字段 + hxqyInfo 列表为 required；ExtInfo optional
 * ({@code minOccurs="0"})。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link HxqyInfo}{@code >} — 核心企业信息列表 ({@code maxOccurs="200"})。
 *       本字段直接复用 T4 已落地的 {@link HxqyInfo}（hxqyName + hxqyCode 2 字段集合 100% 一致），
 *       不新建支撑类型。</li>
 *   <li>{@link ExtInfo} — DataType.xsd 共享扩展块（body.common P2c 已落地）</li>
 * </ul></p>
 *
 * <p><b>对应回执</b>：{@link HxqyCreditAmt3113}（注意 3113 类无 {@code Return} 后缀，
 * 与 XSD complexType 名 {@code hxqyCreditAmt3113} 对齐）。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 层的长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hxqyCreditAmt3112")
@XmlType(name = "hxqyCreditAmt3112", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "queryDate", "hxqyInfoNum",
        "hxqyInfo",
        "extInfo"
})
public class HxqyCreditAmt3112 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "QueryDate", required = true)
    private String queryDate;

    @XmlElement(name = "hxqyInfoNum", required = true)
    private String hxqyInfoNum;

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
     * Returns the query date (查询日期, yyyyMMdd).
     *
     * @return query date
     */
    public String getQueryDate() {
        return queryDate;
    }

    /**
     * Sets the query date.
     *
     * @param v query date (yyyyMMdd)
     */
    public void setQueryDate(final String v) {
        this.queryDate = v;
    }

    /**
     * Returns the count of core enterprises (核心企业数量, 1-2 digits).
     *
     * @return core enterprise count
     */
    public String getHxqyInfoNum() {
        return hxqyInfoNum;
    }

    /**
     * Sets the count of core enterprises.
     *
     * @param v core enterprise count
     */
    public void setHxqyInfoNum(final String v) {
        this.hxqyInfoNum = v;
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
