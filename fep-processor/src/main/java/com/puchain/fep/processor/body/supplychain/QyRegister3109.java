package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3109 企业登记信息报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code qyRegister3109} complexType 的 sequence
 * (8 字段): SerialNo, SendNodeCode, DesNodeCode, qyFlag (required) +
 * hxqyInfo, qyAccLockInfo, PlatInfo, ExtInfo (optional)。</p>
 *
 * <p><b>嵌套类型</b>：
 * <ul>
 *   <li>{@link HxqyInfo3109} {@code hxqyInfo} — 核心企业登记信息段（wrapper 含 hxqyNum + hxqyInfoMx）。
 *       T5 新建独立类，与 T4 {@link HxqyInfo}（3107/3112 共享）字段集合完全不同，
 *       详见 {@link HxqyInfo3109} 类 Javadoc。</li>
 *   <li>{@link QyAccLockInfo} {@code qyAccLockInfo} — 回款锁定企业登记信息段</li>
 *   <li>{@link PlatInfo} {@code PlatInfo} — 平台企业登记信息段（含
 *       {@link com.puchain.fep.processor.body.common.PersonInfo} 联系人）</li>
 *   <li>{@link ExtInfo} {@code ExtInfo} — DataType.xsd 共享扩展块</li>
 * </ul></p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qyRegister3109")
@XmlType(name = "qyRegister3109", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode", "qyFlag",
        "hxqyInfo", "qyAccLockInfo", "platInfo", "extInfo"
})
public class QyRegister3109 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "qyFlag", required = true)
    private String qyFlag;

    @XmlElement(name = "hxqyInfo")
    private HxqyInfo3109 hxqyInfo;

    @XmlElement(name = "qyAccLockInfo")
    private QyAccLockInfo qyAccLockInfo;

    @XmlElement(name = "PlatInfo")
    private PlatInfo platInfo;

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
     * Returns the enterprise classification code (企业分类码).
     *
     * @return classification code
     */
    public String getQyFlag() {
        return qyFlag;
    }

    /**
     * Sets the enterprise classification code.
     *
     * @param v classification code
     */
    public void setQyFlag(final String v) {
        this.qyFlag = v;
    }

    /**
     * Returns the optional core enterprise registration info block (核心企业登记信息列表).
     *
     * @return core enterprise info, or {@code null} if absent
     */
    public HxqyInfo3109 getHxqyInfo() {
        return hxqyInfo;
    }

    /**
     * Sets the core enterprise registration info block.
     *
     * @param v core enterprise info
     */
    public void setHxqyInfo(final HxqyInfo3109 v) {
        this.hxqyInfo = v;
    }

    /**
     * Returns the optional payback-locked enterprise registration info block
     * (回款锁定企业登记信息列表).
     *
     * @return payback-locked info, or {@code null} if absent
     */
    public QyAccLockInfo getQyAccLockInfo() {
        return qyAccLockInfo;
    }

    /**
     * Sets the payback-locked enterprise registration info block.
     *
     * @param v payback-locked info
     */
    public void setQyAccLockInfo(final QyAccLockInfo v) {
        this.qyAccLockInfo = v;
    }

    /**
     * Returns the optional platform enterprise registration info block
     * (平台企业登记信息列表).
     *
     * @return platform info, or {@code null} if absent
     */
    public PlatInfo getPlatInfo() {
        return platInfo;
    }

    /**
     * Sets the platform enterprise registration info block.
     *
     * @param v platform info
     */
    public void setPlatInfo(final PlatInfo v) {
        this.platInfo = v;
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
