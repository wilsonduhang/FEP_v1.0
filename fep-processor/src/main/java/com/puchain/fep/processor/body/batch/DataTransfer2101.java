package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 2101 数据推送报文业务体（PRD v1.3 §4.3 + §4.7 模式 6 + §5.5 数据报送管理）。
 *
 * <p>HNDEMP 主动向外联机构推送数据（信息广播 / 监管数据回填等），外联机构异步
 * 返回 9120 应答（无业务回执）。与 1101 业务方向相反 — 1101 = 外联→HNDEMP，
 * 2101 = HNDEMP→外联。Body 字段集为 1101 的子集（无 Parameters）。</p>
 *
 * <p>字段顺序对应 {@code 2101.xsd} 中 {@code DataTransfer2101} complexType 的 sequence
 * （MainClass → SecondClass → Period → Type → FileDate）。所有字段必填（XSD
 * minOccurs 默认 1，无 minOccurs=0 项 — 与 1101 关键差异）。</p>
 *
 * <p>本类仅承载"业务推送体"，不含 {@code BatchHead2101}（由 P1b
 * {@link com.puchain.fep.converter.model.RequestBusinessHead} 承担 — 2101.xsd
 * 中 BatchHead2101 的类型是 RequestHead 而非 ResponseHead，因为 2101 是 HNDEMP
 * 主动推送而非 1101 请求的回执）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataTransfer2101")
@XmlType(propOrder = {"mainClass", "secondClass", "period", "type", "fileDate"})
public class DataTransfer2101 extends CfxBody implements SerialNoBearing {

    /** 业务大类（XSD MainClass，Token 2-16）。必填。 */
    @XmlElement(name = "MainClass", required = true)
    private String mainClass;

    /** 业务小类（XSD SecondClass，Token 2-16）。必填。 */
    @XmlElement(name = "SecondClass", required = true)
    private String secondClass;

    /** 推送周期（XSD Period，Number 1-2 位）。必填。 */
    @XmlElement(name = "Period", required = true)
    private String period;

    /** 推送类型（XSD Type，Number 1-2 位）。必填。 */
    @XmlElement(name = "Type", required = true)
    private String type;

    /** 文件业务日期（XSD Date，8 位 yyyyMMdd）。必填。 */
    @XmlElement(name = "FileDate", required = true)
    private String fileDate;

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

    /**
     * {@inheritDoc}
     *
     * <p>2101 业务体不携带 SerialNo（XSD {@code DataTransfer2101} sequence 仅 MainClass /
     * SecondClass / Period / Type / FileDate，无 SerialNo 元素）— 业务流水号由 envelope
     * 外层 {@code BatchHead2101}（类型 RequestBusinessHead）的 {@code RequestSerialNo}
     * 字段承担。返回 {@code null} 触发 inbound dispatcher fallback 到 transitionNo，
     * 与 sibling 2102/2103/2104 BATCH 响应体一致。</p>
     */
    @Override
    public String getSerialNo() {
        return null;
    }
}
