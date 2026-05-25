package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归守卫：{@link OutboundCfxEnvelopeBuilder#build} 产出的 CFX envelope 中
 * {@code HEAD/MsgId} 和 {@code HEAD/CorrMsgId} 满足 XSD 类型约束（PRD v1.3 §3.1.3 + §3.2.2）。
 *
 * <p><b>Bug 背景</b>（此测试会在修复前 FAIL，修复后 PASS）：</p>
 * <ul>
 *   <li>{@code MsgId} 原值 {@code "PLACEHOLDER_T6_INJEC"}（非数字） →
 *       {@code Base.xsd} HEAD {@code MsgId} type = {@code Number}（pattern {@code [0-9]*}）
 *       + {@code length=20} → XSD validate 失败 → 生产环境每条出站报文均抛
 *       {@link com.puchain.fep.common.domain.FepErrorCode#OUTBOUND_5102_XSD_VALIDATION_FAILURE}</li>
 *   <li>{@code CorrMsgId} 原值 {@code null} → {@code Base.xsd} HEAD {@code CorrMsgId}
 *       {@code minOccurs=1}（必填）→ XSD validate 失败</li>
 * </ul>
 *
 * <p><b>本测试的独特价值</b>：所有其他 {@code build()} 测试（{@link OutboundCfxEnvelopeBuilderTest}、
 * {@link Outbound9120AckEnvelopeBuilderTest} 等）均 {@code @MockBean XsdValidator}，
 * 不会触发真实 XSD 校验。本测试使用 <b>真实 {@code XsdValidator}</b>（Spring context 注入），
 * 是唯一能在编译期前捕获此类 MsgId/CorrMsgId XSD 合规问题的回归基准。</p>
 *
 * <p>以 1101 报文为 fixture（外联机构数据报送，无响应 Result 字段，
 * 满足 BatchHead 1101 RequestHead 类目约束）。fixture 字段值按
 * {@code DataType.xsd} 实测约束填写：</p>
 * <ul>
 *   <li>{@code MainClass}：Token，minLength=2，maxLength=16 → {@code "EA"}</li>
 *   <li>{@code SecondClass}：Token，minLength=2，maxLength=16 → {@code "EA01"}</li>
 *   <li>{@code Period}：Number，minLength=1，maxLength=2 → {@code "1"}</li>
 *   <li>{@code Type}：Number，minLength=1，maxLength=2 → {@code "1"}</li>
 *   <li>{@code FileDate}：Date，pattern {@code [0-9]{4}(0[1-9]|1[0-2])([0-2][1-9]|...)} → {@code "20260525"}</li>
 * </ul>
 *
 * <p>PRD 依据：§3.1.3 报文标识号（MsgId 20 位全数字）+ §3.2.2 HEAD 固定字段。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundEnvelopeXsdComplianceTest {

    /** 1101 业务体 — 满足 DataTransfer1101 complexType 所有必填字段及各字段 DataType.xsd 约束。 */
    private static final String BODY_XML_1101 = "<DataTransfer1101>"
            + "<MainClass>EA</MainClass>"
            + "<SecondClass>EA01</SecondClass>"
            + "<Period>1</Period>"
            + "<Type>1</Type>"
            + "<FileDate>20260525</FileDate>"
            + "</DataTransfer1101>";

    /** SendOrgCode fixture — OrgCode length=14（DataType.xsd）。 */
    private static final String SEND_ORG_CODE = "BANK0010000001";

    /** EntrustDate fixture — Date pattern yyyyMMdd（DataType.xsd）。 */
    private static final String ENTRUST_DATE = "20260525";

    /** TransitionNo fixture — TransitionNo Number length=8（DataType.xsd）。 */
    private static final String TRANSITION_NO = "00001101";

    /** 真实 {@link OutboundCfxEnvelopeBuilder}（含真实 XsdValidator，不 MockBean）。 */
    @Autowired
    private OutboundCfxEnvelopeBuilder builder;

    @Test
    @DisplayName("build 1101 envelope — 真实 XSD 校验通过，MsgId/CorrMsgId 均为 20 位数字（XSD Bug 回归守卫）")
    void build1101Envelope_realXsdValidator_msgIdAndCorrMsgIdCompliant() {
        final OutboundMessageQueueEntity entity = given1101Entity();
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        final OutboundCfxEnvelopeBuilder.EnvelopeBuildResult built = builder.build(entity, headFields);

        assertThat(built.envelope())
                .as("envelope HEAD/MsgId must be 20 numeric digits (XSD MsgId type = Number + length 20)")
                .containsPattern("<MsgId>[0-9]{20}</MsgId>");
        assertThat(built.envelope())
                .as("envelope HEAD/CorrMsgId must be present and 20 digits (Base.xsd minOccurs=1 + MsgId type)")
                .containsPattern("<CorrMsgId>[0-9]{20}</CorrMsgId>");
        assertThat(built.msgId())
                .as("returned msgId must be 20 digits")
                .matches("[0-9]{20}");
        assertThat(built.envelope())
                .as("returned msgId must equal envelope MsgId (envelope/TLQ/entity three-way consistency)")
                .contains("<MsgId>" + built.msgId() + "</MsgId>");
    }

    private OutboundMessageQueueEntity given1101Entity() {
        final OutboundMessageQueueEntity e = new OutboundMessageQueueEntity();
        e.setMessageType("1101");
        e.setMessageBodyXml(BODY_XML_1101);
        // messageHeadXml 不在 builder 内被反序列化（headFields 由 Runner 上层提供），合成占位即可
        e.setMessageHeadXml("<OutboundHeadFields/>");
        return e;
    }
}
