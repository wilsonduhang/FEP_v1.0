package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3005 对公客户账户状态查询请求报文业务体。
 *
 * <p>字段顺序严格对应 {@code 3005.xsd} 中 {@code qyAccQuery3005} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, qyAccName, qyAccCode, ExtInfo?。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；{@link ExtInfo} 为可选嵌套扩展块。
 * XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qyAccQuery3005")
@XmlType(propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "qyAccName", "qyAccCode", "extInfo"
})
public class QyAccQuery3005 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "qyAccName", required = true)
    private String qyAccName;

    @XmlElement(name = "qyAccCode", required = true)
    private String qyAccCode;

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
     * Returns the corporate account name (对公客户名称).
     *
     * @return corporate account name
     */
    public String getQyAccName() {
        return qyAccName;
    }

    /**
     * Sets the corporate account name.
     *
     * @param v corporate account name
     */
    public void setQyAccName(final String v) {
        this.qyAccName = v;
    }

    /**
     * Returns the corporate account code / USCI (对公客户统一社会信用代码).
     *
     * @return corporate account code
     */
    public String getQyAccCode() {
        return qyAccCode;
    }

    /**
     * Sets the corporate account code.
     *
     * @param v corporate account code
     */
    public void setQyAccCode(final String v) {
        this.qyAccCode = v;
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
