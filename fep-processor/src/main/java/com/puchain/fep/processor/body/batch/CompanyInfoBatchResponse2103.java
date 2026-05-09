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
 * 2103 企业信息批量查询回执报文业务体。
 *
 * <p>外层 wrapper，包含 1..N 个 {@link CompanyInfoBatchItem2103} 结果项。
 * 字段顺序对应 {@code 2103.xsd} 中 {@code CompanyInfoBatchResponse2103} complexType
 * 的 sequence — 仅 1 个 sequence 元素：{@code CompanyInfo} (maxOccurs=unbounded)。</p>
 *
 * <p>本类仅承载"业务回执体"，不含 {@code BatchHead2103}（由 P1b
 * {@link com.puchain.fep.converter.model.ResponseBusinessHead} 承担）。</p>
 *
 * <p><b>注：</b>XSD MSG sequence 中本 outer wrapper minOccurs=0（无结果时整个缺失），
 * 该场景由 dispatcher 层处理；本 POJO 仅在 outer 在场时使用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompanyInfoBatchResponse2103")
@XmlType(propOrder = {"items"})
public class CompanyInfoBatchResponse2103 extends CfxBody implements SerialNoBearing {

    /**
     * 结果项列表，对应 XSD {@code <CompanyInfo>} 重复元素（maxOccurs=unbounded）。
     *
     * <p><b>注：</b>{@code required=true} 表达 XSD {@code minOccurs≥1} 语义；本字段
     * 默认 {@code null}（与 codebase 其他 body List 字段约定一致，参考
     * {@code HxqyCreditAmt3113.creditInfo}），由 caller / dispatcher 在 marshal
     * 前 {@code setItems(...)} 保证非空。</p>
     */
    @XmlElement(name = "CompanyInfo", required = true)
    private List<CompanyInfoBatchItem2103> items;

    public List<CompanyInfoBatchItem2103> getItems() {
        return items;
    }

    public void setItems(final List<CompanyInfoBatchItem2103> v) {
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
