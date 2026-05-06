package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 2102 报文中 {@code DataTransferResult} 单核对结果项。
 *
 * <p>字段顺序严格对应 {@code 2102.xsd} 中 {@code DataTransferResult} complexType 的 sequence：
 * ItemId, MainClass, SecondClass, Period, FileName?, FileDate, Status。</p>
 *
 * <p><b>与 1102 {@link DataTransferCheckBatchItem1102} 的差异：</b>本类 Status 字段为 required
 * （XSD 中无 minOccurs="0"），表达"回执必含状态"语义。</p>
 *
 * <p>所有字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataTransferResult", propOrder = {
        "itemId", "mainClass", "secondClass", "period",
        "fileName", "fileDate", "status"
})
public class DataTransferCheckBatchItem2102 extends CfxBody {

    @XmlElement(name = "ItemId", required = true)
    private String itemId;

    @XmlElement(name = "MainClass", required = true)
    private String mainClass;

    @XmlElement(name = "SecondClass", required = true)
    private String secondClass;

    @XmlElement(name = "Period", required = true)
    private String period;

    @XmlElement(name = "FileName")
    private String fileName;

    @XmlElement(name = "FileDate", required = true)
    private String fileDate;

    @XmlElement(name = "Status", required = true)
    private String status;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(final String v) {
        this.itemId = v;
    }

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String v) {
        this.fileName = v;
    }

    public String getFileDate() {
        return fileDate;
    }

    public void setFileDate(final String v) {
        this.fileDate = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String v) {
        this.status = v;
    }
}
