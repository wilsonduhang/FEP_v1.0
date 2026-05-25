package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.FileInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import com.puchain.fep.processor.body.common.QyAccInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 3105 融资申请信息报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3105.xsd} 中 {@code rzApplyInfo3105} complexType 的 sequence
 * (28 字段)：SerialNo, SendNodeCode, DesNodeCode, BranchBankCode?, ApplyMode, PlatApplyNo,
 * StdBizMode, hxqyName, hxqyCode, rzpzNo, dbqyName?, dbqyCode?, rzqyName, rzqyCode,
 * rzqyAddr?, rzqyPlatNo, rzqyAccInfo?, rzAmtInfo?, SignInfo?, ServiceChargeInfo?,
 * hxqyInterestInfo?, RepayAccInfo?, pzInfo?, zpzInfo?, InvoInfo*, ContractInfo*,
 * AttachFileInfo*, ExtInfo?。</p>
 *
 * <p><b>嵌套类型概览</b> (10 nested 字段)：
 * <ul>
 *   <li>3 × {@link QyAccInfo} (rzqyAccInfo / hxqyInterestInfo / RepayAccInfo) — DataType.xsd 共享</li>
 *   <li>2 × {@link PzInfo} (pzInfo / zpzInfo) — DataType.xsd 共享，body.common P2c 已落地</li>
 *   <li>1 × {@link ExtInfo} — DataType.xsd 共享</li>
 *   <li>1 × {@code List<}{@link FileInfo}{@code >} (AttachFileInfo, maxOccurs=10) — DataType.xsd 共享</li>
 *   <li>1 × {@link RzAmtInfo3105} — 3105 专用支撑</li>
 *   <li>1 × {@link SignInfo} — 3105 专用支撑</li>
 *   <li>1 × {@link ServiceChargeInfo} — 3105 专用支撑</li>
 *   <li>1 × {@code List<}{@link InvoInfo}{@code >} (maxOccurs=10) — 3105 专用支撑</li>
 *   <li>1 × {@code List<}{@link ContractInfo}{@code >} (maxOccurs=10) — 3105 专用支撑</li>
 * </ul></p>
 *
 * <p><b>独立 ContractInfo 类原因</b>: 本类引用的 {@link ContractInfo} (10 字段) 是 3105.xsd
 * 本地 complexType；区别于 {@link ContractInfo3101} 主类 (3101 合同归档 23 字段主体)。
 * 两者 {@code @XmlRootElement} 名称不同 ({@code ContractInfo} vs {@code ContractInfo3101})，
 * JAXB 上下文中并存无冲突。详见 {@link ContractInfo} class Javadoc。</p>
 *
 * <p><b>独立 RzAmtInfo3105 类原因</b>: 本类引用的 {@link RzAmtInfo3105} (9 字段) 与 T1
 * {@link RzAmtInfo3009} (12 字段) 字段集合完全不同，两者拆为独立 POJO；命名带数字后缀
 * 以避免歧义。详见 {@link RzAmtInfo3105} class Javadoc。</p>
 *
 * <p>所有标量字段类型统一为 {@link String}；XSD 层的长度/格式/枚举约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>Getter/setter Javadoc 省略以与 {@link com.puchain.fep.processor.body.common.PzInfo}
 * / {@link ContractInfo3101} 等高字段数 POJO 风格保持一致（避免触发 checkstyle FileLength
 * 上限）。</p>
 *
 * <p><b>SerialNoBearing</b>：实现该接口使本类可注册进 inbound dispatcher BODY_TYPE_REGISTRY（PRD §4.6 受理侧，P4-MSG-K）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rzApplyInfo3105")
@XmlType(name = "rzApplyInfo3105", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode", "branchBankCode",
        "applyMode", "platApplyNo", "stdBizMode",
        "hxqyName", "hxqyCode", "rzpzNo",
        "dbqyName", "dbqyCode",
        "rzqyName", "rzqyCode", "rzqyAddr", "rzqyPlatNo",
        "rzqyAccInfo", "rzAmtInfo", "signInfo", "serviceChargeInfo",
        "hxqyInterestInfo", "repayAccInfo",
        "pzInfo", "zpzInfo",
        "invoInfo", "contractInfo", "attachFileInfo",
        "extInfo"
})
public class RzApplyInfo3105 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "BranchBankCode")
    private String branchBankCode;

    @XmlElement(name = "ApplyMode", required = true)
    private String applyMode;

    @XmlElement(name = "PlatApplyNo", required = true)
    private String platApplyNo;

    @XmlElement(name = "StdBizMode", required = true)
    private String stdBizMode;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "rzpzNo", required = true)
    private String rzpzNo;

    @XmlElement(name = "dbqyName")
    private String dbqyName;

    @XmlElement(name = "dbqyCode")
    private String dbqyCode;

    @XmlElement(name = "rzqyName", required = true)
    private String rzqyName;

    @XmlElement(name = "rzqyCode", required = true)
    private String rzqyCode;

    @XmlElement(name = "rzqyAddr")
    private String rzqyAddr;

    @XmlElement(name = "rzqyPlatNo", required = true)
    private String rzqyPlatNo;

    @XmlElement(name = "rzqyAccInfo")
    private QyAccInfo rzqyAccInfo;

    @XmlElement(name = "rzAmtInfo")
    private RzAmtInfo3105 rzAmtInfo;

    @XmlElement(name = "SignInfo")
    private SignInfo signInfo;

    @XmlElement(name = "ServiceChargeInfo")
    private ServiceChargeInfo serviceChargeInfo;

    @XmlElement(name = "hxqyInterestInfo")
    private QyAccInfo hxqyInterestInfo;

    @XmlElement(name = "RepayAccInfo")
    private QyAccInfo repayAccInfo;

    @XmlElement(name = "pzInfo")
    private PzInfo pzInfo;

    @XmlElement(name = "zpzInfo")
    private PzInfo zpzInfo;

    @XmlElement(name = "InvoInfo")
    private List<InvoInfo> invoInfo;

    @XmlElement(name = "ContractInfo")
    private List<ContractInfo> contractInfo;

    @XmlElement(name = "AttachFileInfo")
    private List<FileInfo> attachFileInfo;

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

    public String getBranchBankCode() {
        return branchBankCode;
    }

    public void setBranchBankCode(final String v) {
        this.branchBankCode = v;
    }

    public String getApplyMode() {
        return applyMode;
    }

    public void setApplyMode(final String v) {
        this.applyMode = v;
    }

    public String getPlatApplyNo() {
        return platApplyNo;
    }

    public void setPlatApplyNo(final String v) {
        this.platApplyNo = v;
    }

    public String getStdBizMode() {
        return stdBizMode;
    }

    public void setStdBizMode(final String v) {
        this.stdBizMode = v;
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

    public String getRzpzNo() {
        return rzpzNo;
    }

    public void setRzpzNo(final String v) {
        this.rzpzNo = v;
    }

    public String getDbqyName() {
        return dbqyName;
    }

    public void setDbqyName(final String v) {
        this.dbqyName = v;
    }

    public String getDbqyCode() {
        return dbqyCode;
    }

    public void setDbqyCode(final String v) {
        this.dbqyCode = v;
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

    public String getRzqyAddr() {
        return rzqyAddr;
    }

    public void setRzqyAddr(final String v) {
        this.rzqyAddr = v;
    }

    public String getRzqyPlatNo() {
        return rzqyPlatNo;
    }

    public void setRzqyPlatNo(final String v) {
        this.rzqyPlatNo = v;
    }

    public QyAccInfo getRzqyAccInfo() {
        return rzqyAccInfo;
    }

    public void setRzqyAccInfo(final QyAccInfo v) {
        this.rzqyAccInfo = v;
    }

    public RzAmtInfo3105 getRzAmtInfo() {
        return rzAmtInfo;
    }

    public void setRzAmtInfo(final RzAmtInfo3105 v) {
        this.rzAmtInfo = v;
    }

    public SignInfo getSignInfo() {
        return signInfo;
    }

    public void setSignInfo(final SignInfo v) {
        this.signInfo = v;
    }

    public ServiceChargeInfo getServiceChargeInfo() {
        return serviceChargeInfo;
    }

    public void setServiceChargeInfo(final ServiceChargeInfo v) {
        this.serviceChargeInfo = v;
    }

    public QyAccInfo getHxqyInterestInfo() {
        return hxqyInterestInfo;
    }

    public void setHxqyInterestInfo(final QyAccInfo v) {
        this.hxqyInterestInfo = v;
    }

    public QyAccInfo getRepayAccInfo() {
        return repayAccInfo;
    }

    public void setRepayAccInfo(final QyAccInfo v) {
        this.repayAccInfo = v;
    }

    public PzInfo getPzInfo() {
        return pzInfo;
    }

    public void setPzInfo(final PzInfo v) {
        this.pzInfo = v;
    }

    public PzInfo getZpzInfo() {
        return zpzInfo;
    }

    public void setZpzInfo(final PzInfo v) {
        this.zpzInfo = v;
    }

    public List<InvoInfo> getInvoInfo() {
        return invoInfo;
    }

    public void setInvoInfo(final List<InvoInfo> v) {
        this.invoInfo = v;
    }

    public List<ContractInfo> getContractInfo() {
        return contractInfo;
    }

    public void setContractInfo(final List<ContractInfo> v) {
        this.contractInfo = v;
    }

    public List<FileInfo> getAttachFileInfo() {
        return attachFileInfo;
    }

    public void setAttachFileInfo(final List<FileInfo> v) {
        this.attachFileInfo = v;
    }

    public ExtInfo getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
