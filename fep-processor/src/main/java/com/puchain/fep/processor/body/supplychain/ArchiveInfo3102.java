package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.FileInfo;
import com.puchain.fep.processor.body.common.PersonInfo;
import com.puchain.fep.processor.body.common.QyAccInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 3102 企业建档信息申请报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3102.xsd} 中 {@code ArchiveInfo3102} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, ApplyMode, GroupName?, GroupCode?, hxqyName,
 * hxqyCode, rzqyName, rzqyCode, rzqyPlatNo?, rzqyCAFilename?, rzqyBaseInfo?, rzqyAccInfo?,
 * rzqyOperatorInfo?, rzqyLegalInfo?, AttachFileInfo? (0..10), ExtInfo?。共 18 字段
 * （8 必填 + 10 可选）。</p>
 *
 * <p>嵌套结构（scan §3.3 + Step 0 实测锁定）：</p>
 * <ul>
 *   <li>{@link RzqyBaseInfo} — 融资企业基本信息（supplychain 局部 complexType, 9 字段）</li>
 *   <li>{@link QyAccInfo} {@code rzqyAccInfo} — 融资企业账户信息（DataType.xsd 共享, 4 字段）</li>
 *   <li>{@link PersonInfo} {@code rzqyOperatorInfo}/{@code rzqyLegalInfo}
 *     — 融资企业经办人/法人信息（DataType.xsd 共享, 各 8 字段）</li>
 *   <li>{@link FileInfo} {@code AttachFileInfo} — 相关文件附件
 *     （DataType.xsd 共享, {@code maxOccurs="10"} → {@code List<FileInfo>}）</li>
 *   <li>{@link ExtInfo} — 自定义扩展信息（body.common, 可选）</li>
 * </ul>
 *
 * <p>所有简单文本字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * <p>Getter/setter Javadoc 省略以与 {@link com.puchain.fep.processor.body.common.PzInfo}
 * 等高字段数 POJO 风格保持一致（避免触发 checkstyle FileLength 上限）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ArchiveInfo3102")
@XmlType(name = "ArchiveInfo3102", propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "applyMode", "groupName", "groupCode",
        "hxqyName", "hxqyCode", "rzqyName", "rzqyCode",
        "rzqyPlatNo", "rzqyCAFilename",
        "rzqyBaseInfo", "rzqyAccInfo", "rzqyOperatorInfo", "rzqyLegalInfo",
        "attachFileInfo", "extInfo"
})
public class ArchiveInfo3102 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "ApplyMode", required = true)
    private String applyMode;

    @XmlElement(name = "GroupName")
    private String groupName;

    @XmlElement(name = "GroupCode")
    private String groupCode;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "rzqyName", required = true)
    private String rzqyName;

    @XmlElement(name = "rzqyCode", required = true)
    private String rzqyCode;

    @XmlElement(name = "rzqyPlatNo")
    private String rzqyPlatNo;

    @XmlElement(name = "rzqyCAFilename")
    private String rzqyCAFilename;

    @XmlElement(name = "rzqyBaseInfo")
    private RzqyBaseInfo rzqyBaseInfo;

    @XmlElement(name = "rzqyAccInfo")
    private QyAccInfo rzqyAccInfo;

    @XmlElement(name = "rzqyOperatorInfo")
    private PersonInfo rzqyOperatorInfo;

    @XmlElement(name = "rzqyLegalInfo")
    private PersonInfo rzqyLegalInfo;

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

    public String getApplyMode() {
        return applyMode;
    }

    public void setApplyMode(final String v) {
        this.applyMode = v;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String v) {
        this.groupName = v;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(final String v) {
        this.groupCode = v;
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

    public String getRzqyPlatNo() {
        return rzqyPlatNo;
    }

    public void setRzqyPlatNo(final String v) {
        this.rzqyPlatNo = v;
    }

    public String getRzqyCAFilename() {
        return rzqyCAFilename;
    }

    public void setRzqyCAFilename(final String v) {
        this.rzqyCAFilename = v;
    }

    public RzqyBaseInfo getRzqyBaseInfo() {
        return rzqyBaseInfo;
    }

    public void setRzqyBaseInfo(final RzqyBaseInfo v) {
        this.rzqyBaseInfo = v;
    }

    public QyAccInfo getRzqyAccInfo() {
        return rzqyAccInfo;
    }

    public void setRzqyAccInfo(final QyAccInfo v) {
        this.rzqyAccInfo = v;
    }

    public PersonInfo getRzqyOperatorInfo() {
        return rzqyOperatorInfo;
    }

    public void setRzqyOperatorInfo(final PersonInfo v) {
        this.rzqyOperatorInfo = v;
    }

    public PersonInfo getRzqyLegalInfo() {
        return rzqyLegalInfo;
    }

    public void setRzqyLegalInfo(final PersonInfo v) {
        this.rzqyLegalInfo = v;
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
