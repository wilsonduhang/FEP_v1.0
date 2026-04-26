package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3009 电子凭证融资结果登记报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3009.xsd} 中 {@code rzReturnInfo3009} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, PlatApplyNo, hxqyName, rzpzNo, rzPhaseCode,
 * rzPhaseInfo?, rzAmtInfo?, dbInfo?, ExtInfo?。共 11 字段（前 7 必填 + 后 4 可选）。</p>
 *
 * <p>{@code rzAmtInfo} 字段为 {@link RzAmtInfo3009}（融资结果信息段，3009.xsd 内局部
 * complexType，与 3105 同名块字段集合不同 → 独立类）；{@code dbInfo} 字段为
 * {@link DbInfo}（融资担保信息段）；{@link ExtInfo} 为可选嵌套扩展块。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rzReturnInfo3009")
@XmlType(name = "rzReturnInfo3009", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "platApplyNo", "hxqyName", "rzpzNo",
        "rzPhaseCode", "rzPhaseInfo",
        "rzAmtInfo", "dbInfo", "extInfo"
})
public class RzReturnInfo3009 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "PlatApplyNo", required = true)
    private String platApplyNo;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "rzpzNo", required = true)
    private String rzpzNo;

    @XmlElement(name = "rzPhaseCode", required = true)
    private String rzPhaseCode;

    @XmlElement(name = "rzPhaseInfo")
    private String rzPhaseInfo;

    @XmlElement(name = "rzAmtInfo")
    private RzAmtInfo3009 rzAmtInfo;

    @XmlElement(name = "dbInfo")
    private DbInfo dbInfo;

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
     * Returns the sending node code (发送方节点代码).
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
     * Returns the destination node code (接收方节点代码).
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
     * Returns the platform financing application business number (平台融资申请业务编号).
     *
     * @return platform application number
     */
    public String getPlatApplyNo() {
        return platApplyNo;
    }

    /**
     * Sets the platform financing application business number.
     *
     * @param v platform application number
     */
    public void setPlatApplyNo(final String v) {
        this.platApplyNo = v;
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
     * Returns the financing voucher number (融资对应凭证编号).
     *
     * @return voucher number
     */
    public String getRzpzNo() {
        return rzpzNo;
    }

    /**
     * Sets the financing voucher number.
     *
     * @param v voucher number
     */
    public void setRzpzNo(final String v) {
        this.rzpzNo = v;
    }

    /**
     * Returns the financing phase code (凭证融资进度码, 2 digits).
     *
     * @return phase code
     */
    public String getRzPhaseCode() {
        return rzPhaseCode;
    }

    /**
     * Sets the financing phase code.
     *
     * @param v phase code
     */
    public void setRzPhaseCode(final String v) {
        this.rzPhaseCode = v;
    }

    /**
     * Returns the financing phase description (凭证融资进度说明, optional).
     *
     * @return phase description, or {@code null} if absent
     */
    public String getRzPhaseInfo() {
        return rzPhaseInfo;
    }

    /**
     * Sets the financing phase description.
     *
     * @param v phase description
     */
    public void setRzPhaseInfo(final String v) {
        this.rzPhaseInfo = v;
    }

    /**
     * Returns the financing result info block (融资结果信息, optional).
     *
     * @return financing result info, or {@code null} if absent
     */
    public RzAmtInfo3009 getRzAmtInfo() {
        return rzAmtInfo;
    }

    /**
     * Sets the financing result info block.
     *
     * @param v financing result info
     */
    public void setRzAmtInfo(final RzAmtInfo3009 v) {
        this.rzAmtInfo = v;
    }

    /**
     * Returns the financing guarantee info block (担保信息, optional).
     *
     * @return guarantee info, or {@code null} if absent
     */
    public DbInfo getDbInfo() {
        return dbInfo;
    }

    /**
     * Sets the financing guarantee info block.
     *
     * @param v guarantee info
     */
    public void setDbInfo(final DbInfo v) {
        this.dbInfo = v;
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
