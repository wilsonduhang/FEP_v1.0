package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.StringReader;
import java.util.Objects;

/**
 * 包内工具类：将 P4 collector 写入 {@code outbound_message_queue.message_head_xml} 列的
 * {@code <OutboundHeadFields>} XML 反序列化为
 * {@link com.puchain.fep.processor.intake.port.OutboundHeadFields} record (P5 T9).
 *
 * <p>序列化格式约定（与 T2 / T4 / T7 fixture 一致）：</p>
 * <pre>
 *   &lt;OutboundHeadFields&gt;
 *       &lt;sendOrgCode&gt;BANK001&lt;/sendOrgCode&gt;
 *       &lt;entrustDate&gt;20260505&lt;/entrustDate&gt;
 *       &lt;transitionNo&gt;00000001&lt;/transitionNo&gt;
 *   &lt;/OutboundHeadFields&gt;
 * </pre>
 *
 * <p><b>注意：</b>该 XML 元素名采用 record component 名（lowercase），与
 * {@code com.puchain.fep.web.outbound.xml.OutboundHeadFieldsXml}（XML 元素 PascalCase
 * SendOrgCode/EntrustDate/TransitionNo）是<b>两套不同的序列化载体</b>。本 parser 服务于
 * P4-P5 衔接路径（fixture / 实测 collector enqueue 行），不与 OutboundHeadFieldsXml 互通。</p>
 *
 * <p>JAXBContext 通过 {@link JaxbContextCache#getForClasses(Class[])} 获取，跨调用复用
 * 进程级 cache（R1 ship）。</p>
 *
 * <p>故障路径：JAXB unmarshal 失败 → 抛
 * {@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}（保留 cause）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class OutboundHeadXmlParser {

    private OutboundHeadXmlParser() {
        // utility class
    }

    /**
     * 反序列化 {@code <OutboundHeadFields>...</OutboundHeadFields>} XML 字符串为
     * {@link OutboundHeadFields} record。
     *
     * @param xml message_head_xml 列内容（非 null）
     * @return 三字段载体（sendOrgCode / entrustDate / transitionNo 均非 null）
     * @throws FepBusinessException unmarshal 失败（{@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}）
     */
    static OutboundHeadFields parse(final String xml) {
        Objects.requireNonNull(xml, "xml");
        final Object unmarshalled;
        try {
            final JAXBContext ctx = JaxbContextCache.getForClasses(Binding.class);
            final Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshalled = unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5106_HEAD_FIELDS_INVALID,
                    "OutboundHeadFields XML 反序列化失败", e);
        }
        if (!(unmarshalled instanceof Binding bound)) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5106_HEAD_FIELDS_INVALID,
                    "OutboundHeadFields XML 根元素名称不匹配 (期望 <OutboundHeadFields>)");
        }
        return new OutboundHeadFields(
                requireField(bound.sendOrgCode, "sendOrgCode"),
                requireField(bound.entrustDate, "entrustDate"),
                requireField(bound.transitionNo, "transitionNo"));
    }

    /**
     * 显式校验 unmarshal 后的 record 字段非 null（替代 record compact constructor 抛 NPE
     * 被 catch 的反向控制流，消除 SpotBugs DCN_NULLPOINTER_EXCEPTION 警告）。
     *
     * @param value     字段值（可能为 null）
     * @param fieldName 字段名（用于错误消息）
     * @return 非 null 字段值
     * @throws FepBusinessException 字段为 null 时（{@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}）
     */
    private static String requireField(final String value, final String fieldName) {
        if (value == null) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5106_HEAD_FIELDS_INVALID,
                    "OutboundHeadFields XML 缺少必填字段: " + fieldName);
        }
        return value;
    }

    /**
     * JAXB 绑定容器：与 record component 名（lowercase）对齐的 XML 字段映射。
     *
     * <p>之所以用内部 binding 类而不是直接给 record 加 JAXB 注解，因为
     * {@link OutboundHeadFields} 位于 {@code fep-processor.intake.port} 跨模块契约包，
     * ArchUnit 守护其依赖最小化（仅 {@link Objects}）。本 binding 类承载 JAXB 注解
     * 不污染契约层。</p>
     */
    @XmlRootElement(name = "OutboundHeadFields")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class Binding {

        @XmlElement(name = "sendOrgCode")
        private String sendOrgCode;

        @XmlElement(name = "entrustDate")
        private String entrustDate;

        @XmlElement(name = "transitionNo")
        private String transitionNo;

        Binding() {
            // JAXB
        }
    }
}
