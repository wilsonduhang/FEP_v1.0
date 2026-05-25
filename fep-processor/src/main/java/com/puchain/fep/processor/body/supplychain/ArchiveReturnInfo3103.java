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
 * 3103 企业建档信息回执报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3103.xsd} 中 {@code ArchiveReturnInfo3103} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, CreationRetCode, CreationRetInfo?, hxqyName,
 * hxqyCode, rzqyName, rzqyCode, rzqyBankCusCode?, BranchBankName?, CusManagerName?,
 * CusManagerPhone?, rzAmt?, rzRate?, sxRate?, dbRate?, StartDate?, EndDate?, ExtInfo?。
 * 共 20 字段（8 必填 + 12 可选）。</p>
 *
 * <p>{@link ExtInfo} 为可选嵌套扩展块（{@code body.common.ExtInfo}）。所有文本字段
 * Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>Getter/setter Javadoc 省略以与 {@link com.puchain.fep.processor.body.common.PzInfo}
 * 等高字段数 POJO 风格保持一致（避免触发 checkstyle FileLength 上限）。</p>
 *
 * <p><b>SerialNoBearing</b>：实现该接口使本类可注册进 inbound dispatcher BODY_TYPE_REGISTRY（PRD §4.6 受理侧，P4-MSG-K）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ArchiveReturnInfo3103")
@XmlType(name = "ArchiveReturnInfo3103", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "creationRetCode", "creationRetInfo",
        "hxqyName", "hxqyCode", "rzqyName", "rzqyCode",
        "rzqyBankCusCode", "branchBankName",
        "cusManagerName", "cusManagerPhone",
        "rzAmt", "rzRate", "sxRate", "dbRate",
        "startDate", "endDate", "extInfo"
})
public class ArchiveReturnInfo3103 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "CreationRetCode", required = true)
    private String creationRetCode;

    @XmlElement(name = "CreationRetInfo")
    private String creationRetInfo;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "rzqyName", required = true)
    private String rzqyName;

    @XmlElement(name = "rzqyCode", required = true)
    private String rzqyCode;

    @XmlElement(name = "rzqyBankCusCode")
    private String rzqyBankCusCode;

    @XmlElement(name = "BranchBankName")
    private String branchBankName;

    @XmlElement(name = "CusManagerName")
    private String cusManagerName;

    @XmlElement(name = "CusManagerPhone")
    private String cusManagerPhone;

    @XmlElement(name = "rzAmt")
    private String rzAmt;

    @XmlElement(name = "rzRate")
    private String rzRate;

    @XmlElement(name = "sxRate")
    private String sxRate;

    @XmlElement(name = "dbRate")
    private String dbRate;

    @XmlElement(name = "StartDate")
    private String startDate;

    @XmlElement(name = "EndDate")
    private String endDate;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    public String getSendNodeCode() {
        return sendNodeCode;
    }

    public void setSendNodeCode(final String v) {
        this.sendNodeCode = v;
    }

    public String getDesNodeCode() {
        return desNodeCode;
    }

    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    public String getCreationRetCode() {
        return creationRetCode;
    }

    public void setCreationRetCode(final String v) {
        this.creationRetCode = v;
    }

    public String getCreationRetInfo() {
        return creationRetInfo;
    }

    public void setCreationRetInfo(final String v) {
        this.creationRetInfo = v;
    }

    public String getHxqyName() {
        return hxqyName;
    }

    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    public String getHxqyCode() {
        return hxqyCode;
    }

    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
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

    public String getRzqyBankCusCode() {
        return rzqyBankCusCode;
    }

    public void setRzqyBankCusCode(final String v) {
        this.rzqyBankCusCode = v;
    }

    public String getBranchBankName() {
        return branchBankName;
    }

    public void setBranchBankName(final String v) {
        this.branchBankName = v;
    }

    public String getCusManagerName() {
        return cusManagerName;
    }

    public void setCusManagerName(final String v) {
        this.cusManagerName = v;
    }

    public String getCusManagerPhone() {
        return cusManagerPhone;
    }

    public void setCusManagerPhone(final String v) {
        this.cusManagerPhone = v;
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

    public String getSxRate() {
        return sxRate;
    }

    public void setSxRate(final String v) {
        this.sxRate = v;
    }

    public String getDbRate() {
        return dbRate;
    }

    public void setDbRate(final String v) {
        this.dbRate = v;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(final String v) {
        this.startDate = v;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String v) {
        this.endDate = v;
    }

    public ExtInfo getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
