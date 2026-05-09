package com.puchain.fep.processor.body.batch;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * 2104 企业信息查询授权书批量回执报文业务体。
 *
 * <p>外层 wrapper，包含 1..N 个 {@link CompanyAuthFileBatchItem2104} 备案回执项。
 * 字段顺序对应 {@code 2104.xsd} 中 {@code CompanyAuthFileBatchResponse2104} complexType
 * 的 sequence — 仅 1 个 sequence 元素：{@code CompanyAuthFileResponse} (maxOccurs=unbounded)。</p>
 *
 * <p>注：在外层 MSG 中本元素 minOccurs=0；空回执由 dispatcher 决定不发送本元素。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyAuthFileBatchResponse2104")
@XmlType(propOrder = {"items"})
public class CompanyAuthFileBatchResponse2104 extends CfxBody implements SerialNoBearing {

    /**
     * 备案回执项列表，对应 XSD {@code <CompanyAuthFileResponse>} 重复元素（maxOccurs=unbounded）。
     *
     * <p><b>注：</b>{@code required=true} 表达 XSD {@code minOccurs≥1} 语义；本字段
     * 默认 {@code null}，由 caller 在 marshal 前保证非空。</p>
     */
    @XmlElement(name = "CompanyAuthFileResponse", required = true)
    private List<CompanyAuthFileBatchItem2104> items;

    public List<CompanyAuthFileBatchItem2104> getItems() {
        return items;
    }

    public void setItems(final List<CompanyAuthFileBatchItem2104> v) {
        this.items = v;
    }

    /**
     * BATCH 响应无顶层 SerialNo（逐条流水号位于 sub-records 内），
     * 返回 {@code null} 让 {@code InboundMessageDispatcher.extractSerialNo}
     * fallback 到 transitionNo（E-3 invariant — 注册项必 implements
     * {@link SerialNoBearing}，BATCH 类约定 null/空串触发 fallback）。
     */
    @Override
    public String getSerialNo() {
        return null;
    }
}
