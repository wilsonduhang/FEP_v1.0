package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 3113 核心企业授信余额查询回执报文业务体 (PRD §4.4)。
 *
 * <p><b>命名约定</b>：本类名 {@code HxqyCreditAmt3113} <b>无 {@code Return} 后缀</b>，
 * 与 XSD 中 complexType 名 {@code hxqyCreditAmt3113} 严格对齐。3112/3113 配对在 XSD 层
 * 即采用同一前缀 ({@code hxqyCreditAmt}) + 不同 msgNo 的命名风格，区别于 3107/3108
 * 的 {@code pzCheckQuery3107} / {@code pzCheckQueryReturn3108} 命名风格。</p>
 *
 * <p>字段顺序严格对应 {@code 3113.xsd} 中 {@code hxqyCreditAmt3113} complexType 的 sequence
 * (7 字段): SerialNo, SendNodeCode, DesNodeCode, QueryDate, CreditInfoNum,
 * CreditInfo[1..200], ExtInfo?。前 5 字段 + CreditInfo 列表为 required；ExtInfo optional
 * ({@code minOccurs="0"})。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@code List<}{@link CreditInfo}{@code >} — 核心企业授信信息列表
 *       ({@code maxOccurs="200"})，supplychain 局部支撑类型</li>
 *   <li>{@link ExtInfo} — DataType.xsd 共享扩展块（body.common P2c 已落地）</li>
 * </ul></p>
 *
 * <p><b>对应请求</b>：{@link HxqyCreditAmt3112}。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 层的长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p><b>SerialNoBearing</b>：实现该接口使本类可注册进 inbound dispatcher BODY_TYPE_REGISTRY（PRD §4.6 受理侧，P4-MSG-K）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hxqyCreditAmt3113")
@XmlType(name = "hxqyCreditAmt3113", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "queryDate", "creditInfoNum",
        "creditInfo",
        "extInfo"
})
public class HxqyCreditAmt3113 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "QueryDate", required = true)
    private String queryDate;

    @XmlElement(name = "CreditInfoNum", required = true)
    private String creditInfoNum;

    @XmlElement(name = "CreditInfo", required = true)
    private List<CreditInfo> creditInfo;

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
    public String getCreditInfoNum() {
        return creditInfoNum;
    }

    /**
     * Sets the count of core enterprises.
     *
     * @param v core enterprise count
     */
    public void setCreditInfoNum(final String v) {
        this.creditInfoNum = v;
    }

    /**
     * Returns the core enterprise credit info list
     * (核心企业授信信息列表, 1-200 entries).
     *
     * @return credit info list
     */
    public List<CreditInfo> getCreditInfo() {
        return creditInfo;
    }

    /**
     * Sets the core enterprise credit info list.
     *
     * @param v credit info list
     */
    public void setCreditInfo(final List<CreditInfo> v) {
        this.creditInfo = v;
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
