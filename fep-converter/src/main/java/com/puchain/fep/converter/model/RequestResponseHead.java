package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 双向通用转发业务头。参见 PRD v1.3 §4.1.1 通用转发，对齐
 * Base.xsd complexType (line 124-155)：{@code Result} minOccurs=0 +
 * {@code AddWord} minOccurs=0。
 *
 * <p>专用于 3020/3115/3120 通用转发报文族 —— 单 msgNo 同时承载请求与
 * 应答语义，{@code Result}/{@code AddWord} 均为可选 (XSD minOccurs=0，
 * 无格式约束)。继承 {@link RequestBusinessHead} 的 3 字段
 * (SendOrgCode/EntrustDate/TransitionNo 按原值回填)，复用既有
 * {@code ResponseBusinessHead extends RequestBusinessHead} 范式。</p>
 *
 * <p><b>setter 语义与 {@link ResponseBusinessHead} 的区别</b>：本类
 * {@code Result}/{@code AddWord} setter 为 <b>null-passthrough only</b>，
 * 不做 Pattern / 长度校验（XSD minOccurs=0 无格式约束）；而
 * {@link ResponseBusinessHead} 的 {@code Result} 为 5 位数字强校验
 * (RESULT_PATTERN)、{@code AddWord} 为 ≤200 字符长度校验。混用会破坏
 * 通用转发可选语义，故独立成类。</p>
 *
 * <p><b>JAXB propOrder 规则</b>：子类 propOrder 仅列子类自有属性，父类
 * 属性由父类自身 propOrder 输出。包含父类
 * sendOrgCode/entrustDate/transitionNo 会在 JAXBContext 注册时触发
 * IllegalAnnotationsException（P5 T4 教训，{@link ResponseBusinessHead}
 * 已固化此范式）。本类由 P4-MSG-G T1 引入，后续 T3 dispatcher 将其作为
 * 第 5 类 wire-shape head 引用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
// JAXB propOrder rule: subclass propOrder lists ONLY subclass-own properties; parent properties
// are emitted via the parent's own propOrder. Including parent's sendOrgCode/entrustDate/transitionNo
// here triggers IllegalAnnotationsException when this class is registered in a JAXBContext
// (P5 T4 lesson; ResponseBusinessHead already fixed this pattern).
@XmlType(propOrder = {"result", "addWord"})
public class RequestResponseHead extends RequestBusinessHead {

    private String result;
    private String addWord;

    /**
     * 获取业务处理结果。
     *
     * @return 业务处理结果，可选 (XSD minOccurs=0)，无格式约束，可能为 {@code null}
     */
    @XmlElement(name = "Result", required = false)
    public String getResult() {
        return result;
    }

    /**
     * 设置业务处理结果。null-passthrough only —— 不做 Pattern 校验
     * (XSD minOccurs=0 无格式约束，与 {@link ResponseBusinessHead}
     * 的 5 位数字强校验区分)。
     *
     * @param v 业务处理结果，允许 {@code null}
     */
    public void setResult(final String v) {
        // null-passthrough only, no Pattern check (XSD minOccurs=0 has no format restriction)
        this.result = v;
    }

    /**
     * 获取业务处理结果附言。
     *
     * @return 业务处理结果附言，可选 (XSD minOccurs=0)，可能为 {@code null}
     */
    @XmlElement(name = "AddWord", required = false)
    public String getAddWord() {
        return addWord;
    }

    /**
     * 设置业务处理结果附言。null-passthrough only —— 不做长度校验
     * (XSD minOccurs=0 无格式约束，长度由调用方负责，与
     * {@link ResponseBusinessHead} 的 ≤200 字符强校验区分)。
     *
     * @param v 业务处理结果附言，允许 {@code null}
     */
    public void setAddWord(final String v) {
        // null-passthrough only, no length check (caller responsibility)
        this.addWord = v;
    }
}
