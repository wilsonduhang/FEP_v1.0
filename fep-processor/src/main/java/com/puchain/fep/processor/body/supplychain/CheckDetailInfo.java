package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Local complexType {@code CheckDetailInfo} — 资金当日发生额交易明细（17 字段）。
 *
 * <p>字段顺序严格对应 {@code 3116.xsd} 中 {@code CheckDetailInfo} complexType 的 sequence：
 * sid, PlatNodeCode, pzNo?, BizType, BillNo?, rzqyName, rzqyCode, rzAmt, rzRate,
 * rzStartDate, rzEndDate, Amt, RepayStyle?, lxAmt?, dbAmt?, PlatServiceAmt?,
 * CheckMemo?。10 字段 required（sid, PlatNodeCode, BizType, rzqyName, rzqyCode,
 * rzAmt, rzRate, rzStartDate, rzEndDate, Amt）+ 7 字段 optional
 * （{@code minOccurs="0"}）。</p>
 *
 * <p>嵌入在 {@link BankCheckDay3116#getCheckDetailInfo()}（{@code maxOccurs="200"}）下，
 * 单条对应一笔银行当日资金发生额明细。仅 3116 报文使用本类型。</p>
 *
 * <p><b>XSD default 值</b>：{@code Amt}, {@code lxAmt}, {@code dbAmt},
 * {@code PlatServiceAmt} 在 XSD 内带 {@code default="0.00"}。本 POJO 未在
 * Java 层重复设置默认值；序列化时 {@code null} 字段会被 JAXB 省略，由 XSD 校验
 * 阶段补默认。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。
 * 因 17 字段 getter/setter 数量较多，本类对齐 {@link PlatInfo} 简写惯例：
 * 仅保留类与字段级语义注释，省略 getter/setter Javadoc
 * （Checkstyle {@code allowMissingPropertyJavadoc=true} 允许）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CheckDetailInfo")
@XmlType(name = "CheckDetailInfo", propOrder = {
        "sid", "platNodeCode", "pzNo",
        "bizType", "billNo",
        "rzqyName", "rzqyCode",
        "rzAmt", "rzRate",
        "rzStartDate", "rzEndDate",
        "amt", "repayStyle",
        "lxAmt", "dbAmt", "platServiceAmt",
        "checkMemo"
})
public class CheckDetailInfo extends CfxBody {

    @XmlElement(name = "sid", required = true)
    private String sid;

    @XmlElement(name = "PlatNodeCode", required = true)
    private String platNodeCode;

    @XmlElement(name = "pzNo")
    private String pzNo;

    @XmlElement(name = "BizType", required = true)
    private String bizType;

    @XmlElement(name = "BillNo")
    private String billNo;

    @XmlElement(name = "rzqyName", required = true)
    private String rzqyName;

    @XmlElement(name = "rzqyCode", required = true)
    private String rzqyCode;

    @XmlElement(name = "rzAmt", required = true)
    private String rzAmt;

    @XmlElement(name = "rzRate", required = true)
    private String rzRate;

    @XmlElement(name = "rzStartDate", required = true)
    private String rzStartDate;

    @XmlElement(name = "rzEndDate", required = true)
    private String rzEndDate;

    @XmlElement(name = "Amt", required = true)
    private String amt;

    @XmlElement(name = "RepayStyle")
    private String repayStyle;

    @XmlElement(name = "lxAmt")
    private String lxAmt;

    @XmlElement(name = "dbAmt")
    private String dbAmt;

    @XmlElement(name = "PlatServiceAmt")
    private String platServiceAmt;

    @XmlElement(name = "CheckMemo")
    private String checkMemo;

    public String getSid() {
        return sid;
    }

    public void setSid(final String v) {
        this.sid = v;
    }

    public String getPlatNodeCode() {
        return platNodeCode;
    }

    public void setPlatNodeCode(final String v) {
        this.platNodeCode = v;
    }

    public String getPzNo() {
        return pzNo;
    }

    public void setPzNo(final String v) {
        this.pzNo = v;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(final String v) {
        this.bizType = v;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(final String v) {
        this.billNo = v;
    }

    public String getRzqyName() {
        return rzqyName;
    }

    public void setRzqyName(final String v) {
        this.rzqyName = v;
    }

    public String getRzqyCode() {
        return rzqyCode;
    }

    public void setRzqyCode(final String v) {
        this.rzqyCode = v;
    }

    public String getRzAmt() {
        return rzAmt;
    }

    public void setRzAmt(final String v) {
        this.rzAmt = v;
    }

    public String getRzRate() {
        return rzRate;
    }

    public void setRzRate(final String v) {
        this.rzRate = v;
    }

    public String getRzStartDate() {
        return rzStartDate;
    }

    public void setRzStartDate(final String v) {
        this.rzStartDate = v;
    }

    public String getRzEndDate() {
        return rzEndDate;
    }

    public void setRzEndDate(final String v) {
        this.rzEndDate = v;
    }

    public String getAmt() {
        return amt;
    }

    public void setAmt(final String v) {
        this.amt = v;
    }

    public String getRepayStyle() {
        return repayStyle;
    }

    public void setRepayStyle(final String v) {
        this.repayStyle = v;
    }

    public String getLxAmt() {
        return lxAmt;
    }

    public void setLxAmt(final String v) {
        this.lxAmt = v;
    }

    public String getDbAmt() {
        return dbAmt;
    }

    public void setDbAmt(final String v) {
        this.dbAmt = v;
    }

    public String getPlatServiceAmt() {
        return platServiceAmt;
    }

    public void setPlatServiceAmt(final String v) {
        this.platServiceAmt = v;
    }

    public String getCheckMemo() {
        return checkMemo;
    }

    public void setCheckMemo(final String v) {
        this.checkMemo = v;
    }
}
