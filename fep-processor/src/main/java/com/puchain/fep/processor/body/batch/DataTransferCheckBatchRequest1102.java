package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 1102 外联机构数据报送核对请求报文业务体。
 *
 * <p>外层 wrapper，包含 1..N 个 {@link DataTransferCheckBatchItem1102} 核对项。
 * 字段顺序对应 {@code 1102.xsd} 中 {@code DataTransferCheckRequest1102} complexType
 * 的 sequence — 仅 1 个 sequence 元素：{@code DataTransferCheck} (maxOccurs=unbounded)。</p>
 *
 * <p>本类仅承载"业务请求体"，不含 {@code BatchHead1102}（由 P1b
 * {@link com.puchain.fep.converter.model.RequestBusinessHead} 承担）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataTransferCheckRequest1102")
@XmlType(propOrder = {"items"})
public class DataTransferCheckBatchRequest1102 extends CfxBody {

    /**
     * 核对项列表，对应 XSD {@code <DataTransferCheck>} 重复元素（maxOccurs=unbounded）。
     *
     * <p><b>注：</b>{@code required=true} 表达 XSD {@code minOccurs≥1} 语义；本字段
     * 默认 {@code null}（与 codebase 其他 body List 字段约定一致），由 caller / dispatcher
     * 在 marshal 前 {@code setItems(...)} 保证非空。</p>
     */
    @XmlElement(name = "DataTransferCheck", required = true)
    private List<DataTransferCheckBatchItem1102> items;

    public List<DataTransferCheckBatchItem1102> getItems() {
        return items;
    }

    public void setItems(final List<DataTransferCheckBatchItem1102> v) {
        this.items = v;
    }
}
