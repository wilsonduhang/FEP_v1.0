package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code qyAccLockInfoMx} — 回款锁定企业明细信息（10 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code qyAccLockInfoMx} complexType 的 sequence:
 * MonitorState, AccMonitorNo, qyName, PayerName (required) +
 * BeginDate, EndDate, AmtMin, AmtMax, Memo, Filename (optional)。</p>
 *
 * <p>本类作为 {@link QyAccLockInfo#getQyAccLockInfoMx()} 的列表元素出现，
 * {@code maxOccurs="20"} 限制每条 3109 报文最多 20 个回款锁定企业明细。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qyAccLockInfoMx")
@XmlType(name = "qyAccLockInfoMx", propOrder = {
        "monitorState", "accMonitorNo", "qyName", "payerName",
        "beginDate", "endDate", "amtMin", "amtMax", "memo", "filename"
})
public class QyAccLockInfoMx extends CfxBody {

    @XmlElement(name = "MonitorState", required = true)
    private String monitorState;

    @XmlElement(name = "AccMonitorNo", required = true)
    private String accMonitorNo;

    @XmlElement(name = "qyName", required = true)
    private String qyName;

    @XmlElement(name = "PayerName", required = true)
    private String payerName;

    @XmlElement(name = "BeginDate")
    private String beginDate;

    @XmlElement(name = "EndDate")
    private String endDate;

    @XmlElement(name = "AmtMin")
    private String amtMin;

    @XmlElement(name = "AmtMax")
    private String amtMax;

    @XmlElement(name = "Memo")
    private String memo;

    @XmlElement(name = "Filename")
    private String filename;

    /**
     * Returns the monitoring state code (状态识别码).
     *
     * @return monitoring state code
     */
    public String getMonitorState() {
        return monitorState;
    }

    /**
     * Sets the monitoring state code.
     *
     * @param v monitoring state code
     */
    public void setMonitorState(final String v) {
        this.monitorState = v;
    }

    /**
     * Returns the bank-unique application number (银行唯一标识申请号).
     *
     * @return account monitor number
     */
    public String getAccMonitorNo() {
        return accMonitorNo;
    }

    /**
     * Sets the bank-unique application number.
     *
     * @param v account monitor number
     */
    public void setAccMonitorNo(final String v) {
        this.accMonitorNo = v;
    }

    /**
     * Returns the monitored enterprise name (回款监控企业名称).
     *
     * @return enterprise name
     */
    public String getQyName() {
        return qyName;
    }

    /**
     * Sets the monitored enterprise name.
     *
     * @param v enterprise name
     */
    public void setQyName(final String v) {
        this.qyName = v;
    }

    /**
     * Returns the payer enterprise name (付款企业名称).
     *
     * @return payer name
     */
    public String getPayerName() {
        return payerName;
    }

    /**
     * Sets the payer enterprise name.
     *
     * @param v payer name
     */
    public void setPayerName(final String v) {
        this.payerName = v;
    }

    /**
     * Returns the optional monitoring start date (监控开始日, yyyyMMdd).
     *
     * @return begin date, or {@code null} if absent
     */
    public String getBeginDate() {
        return beginDate;
    }

    /**
     * Sets the monitoring start date.
     *
     * @param v begin date (yyyyMMdd)
     */
    public void setBeginDate(final String v) {
        this.beginDate = v;
    }

    /**
     * Returns the optional monitoring end date (监控截止日, yyyyMMdd).
     *
     * @return end date, or {@code null} if absent
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * Sets the monitoring end date.
     *
     * @param v end date (yyyyMMdd)
     */
    public void setEndDate(final String v) {
        this.endDate = v;
    }

    /**
     * Returns the optional minimum amount (最小金额).
     *
     * @return min amount, or {@code null} if absent
     */
    public String getAmtMin() {
        return amtMin;
    }

    /**
     * Sets the minimum amount.
     *
     * @param v min amount
     */
    public void setAmtMin(final String v) {
        this.amtMin = v;
    }

    /**
     * Returns the optional maximum amount (最大金额).
     *
     * @return max amount, or {@code null} if absent
     */
    public String getAmtMax() {
        return amtMax;
    }

    /**
     * Sets the maximum amount.
     *
     * @param v max amount
     */
    public void setAmtMax(final String v) {
        this.amtMax = v;
    }

    /**
     * Returns the optional monitoring/payback remarks (监控或回款备注说明).
     *
     * @return memo, or {@code null} if absent
     */
    public String getMemo() {
        return memo;
    }

    /**
     * Sets the monitoring/payback remarks.
     *
     * @param v memo
     */
    public void setMemo(final String v) {
        this.memo = v;
    }

    /**
     * Returns the optional file name (文件名).
     *
     * @return file name, or {@code null} if absent
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the file name.
     *
     * @param v file name
     */
    public void setFilename(final String v) {
        this.filename = v;
    }
}
