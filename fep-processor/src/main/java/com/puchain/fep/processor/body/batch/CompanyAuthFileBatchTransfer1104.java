package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 1104 企业信息查询授权书批量发送报文业务体。
 *
 * <p>外层 wrapper，包含 1..N 个 {@link CompanyAuthFileBatchItem1104} 授权书项。
 * 字段顺序对应 {@code 1104.xsd} 中 {@code CompanyAuthFileBatchTransfer1104} complexType
 * 的 sequence — 仅 1 个 sequence 元素：{@code CompanyAuthFile} (maxOccurs=unbounded)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyAuthFileBatchTransfer1104")
@XmlType(propOrder = {"items"})
public class CompanyAuthFileBatchTransfer1104 extends CfxBody {

    /**
     * 授权书项列表，对应 XSD {@code <CompanyAuthFile>} 重复元素（maxOccurs=unbounded）。
     *
     * <p><b>注：</b>{@code required=true} 表达 XSD {@code minOccurs≥1} 语义；本字段
     * 默认 {@code null}，由 caller 在 marshal 前保证非空。</p>
     */
    @XmlElement(name = "CompanyAuthFile", required = true)
    private List<CompanyAuthFileBatchItem1104> items;

    public List<CompanyAuthFileBatchItem1104> getItems() {
        return items;
    }

    public void setItems(final List<CompanyAuthFileBatchItem1104> v) {
        this.items = v;
    }
}
