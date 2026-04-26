package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3120 供应链非实时业务通用转发报文业务体 (PRD §4.4)。
 *
 * <p>字段顺序严格对应 {@code 3120.xsd} 中 {@code Forward3120} complexType 的 sequence
 * （7 字段）：SerialNo, SrcNodeCode, DesNodeCode, BusinessNo?, Parameters?,
 * Content, ExtInfo?。前 3 字段 + Content 必填（与 3020 的可选 Content 不同），
 * BusinessNo / Parameters / ExtInfo 可选。</p>
 *
 * <p>3120 与 3020 结构相同（同 7 字段名），仅 Content 字段必填性不同；MSG 业务头
 * 分别为 {@code BatchHead3120} (RequestHead) 与 {@code RealHead3020}
 * (RequestResponseHead) 的差异由 service 层处理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Forward3120")
@XmlType(name = "Forward3120", propOrder = {
        "serialNo", "srcNodeCode", "desNodeCode",
        "businessNo", "parameters", "content", "extInfo"
})
public class Forward3120 extends CfxBody {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SrcNodeCode", required = true)
    private String srcNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "BusinessNo")
    private String businessNo;

    @XmlElement(name = "Parameters")
    private String parameters;

    @XmlElement(name = "Content", required = true)
    private String content;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    public String getSrcNodeCode() {
        return srcNodeCode;
    }

    public void setSrcNodeCode(final String v) {
        this.srcNodeCode = v;
    }

    public String getDesNodeCode() {
        return desNodeCode;
    }

    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(final String v) {
        this.businessNo = v;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(final String v) {
        this.parameters = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String v) {
        this.content = v;
    }

    public ExtInfo getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
