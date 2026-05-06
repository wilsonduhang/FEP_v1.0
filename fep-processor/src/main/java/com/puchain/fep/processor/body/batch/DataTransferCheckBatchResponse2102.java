package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 2102 外联机构数据报送核对回执报文业务体。
 *
 * <p>外层 wrapper，包含 1..N 个 {@link DataTransferCheckBatchItem2102} 核对结果项。
 * 字段顺序对应 {@code 2102.xsd} 中 {@code DataTransferCheckResponse2102} complexType
 * 的 sequence — 仅 1 个 sequence 元素：{@code DataTransferResult} (maxOccurs=unbounded)。</p>
 *
 * <p>注：在外层 MSG 中此元素 minOccurs=0（PRD XSD 显式标记），但其内部 items 列表 minOccurs=1。
 * 本类不模型外层可选性，仅承载有结果项的回执；空回执由 dispatcher 决定不发送本元素。</p>
 *
 * <p>本类仅承载"业务回执体"，不含 {@code BatchHead2102}（由 P1b
 * {@link com.puchain.fep.converter.model.ResponseBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataTransferCheckResponse2102")
@XmlType(propOrder = {"items"})
public class DataTransferCheckBatchResponse2102 extends CfxBody {

    /**
     * 核对结果项列表，对应 XSD {@code <DataTransferResult>} 重复元素（maxOccurs=unbounded）。
     *
     * <p><b>注：</b>{@code required=true} 表达 XSD {@code minOccurs≥1} 语义；本字段
     * 默认 {@code null}，由 caller 在 marshal 前保证非空。</p>
     */
    @XmlElement(name = "DataTransferResult", required = true)
    private List<DataTransferCheckBatchItem2102> items;

    public List<DataTransferCheckBatchItem2102> getItems() {
        return items;
    }

    public void setItems(final List<DataTransferCheckBatchItem2102> v) {
        this.items = v;
    }
}
