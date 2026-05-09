package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 1101 外联机构数据报送报文业务体（PRD v1.3 §4.3 + §4.7 模式 3 + §5.5 数据报送管理）。
 *
 * <p>外联机构主动向 HNDEMP 报送各类监管数据（流水贷 / 反诈自名单 / 一网通办 /
 * EAST / CPS 等），HNDEMP 异步返回 9120 应答（无业务回执）。报送文件本身的 SM3
 * 哈希校验由 CommonHead.FileContentHash 字段承担（见 Base.xsd HEAD complexType），
 * 不在 Body 字段集内。</p>
 *
 * <p>字段顺序对应 {@code 1101.xsd} 中 {@code DataTransfer1101} complexType 的 sequence
 * （MainClass → SecondClass → Period → Type → FileDate → Parameters?）。</p>
 *
 * <p>本类仅承载"业务请求体"，不含 {@code BatchHead1101}（由 P1b
 * {@link com.puchain.fep.converter.model.RequestBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataTransfer1101")
@XmlType(propOrder = {"mainClass", "secondClass", "period", "type", "fileDate", "parameters"})
public class DataTransfer1101 extends CfxBody {

    /** 业务大类（XSD MainClass，Token 2-16）。必填。 */
    @XmlElement(name = "MainClass", required = true)
    private String mainClass;

    /** 业务小类（XSD SecondClass，Token 2-16）。必填。 */
    @XmlElement(name = "SecondClass", required = true)
    private String secondClass;

    /** 报送周期（XSD Period，Number 1-2 位）。必填。 */
    @XmlElement(name = "Period", required = true)
    private String period;

    /** 报送类型（XSD Type，Number 1-2 位）。必填。 */
    @XmlElement(name = "Type", required = true)
    private String type;

    /** 文件业务日期（XSD Date，8 位 yyyyMMdd）。必填。 */
    @XmlElement(name = "FileDate", required = true)
    private String fileDate;

    /** 参数（XSD Parameters，Text ≤ 2000）。可选 — null 时 marshal 不输出元素（XSD minOccurs=0）。 */
    @XmlElement(name = "Parameters")
    private String parameters;

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(final String v) {
        this.mainClass = v;
    }

    public String getSecondClass() {
        return secondClass;
    }

    public void setSecondClass(final String v) {
        this.secondClass = v;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(final String v) {
        this.period = v;
    }

    public String getType() {
        return type;
    }

    public void setType(final String v) {
        this.type = v;
    }

    public String getFileDate() {
        return fileDate;
    }

    public void setFileDate(final String v) {
        this.fileDate = v;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(final String v) {
        this.parameters = v;
    }
}
