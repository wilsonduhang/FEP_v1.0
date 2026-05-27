package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-I T5 — P0 缺口闭合证明：2101 模式 6 ack 装配段端到端 IT（{@code @SpringBootTest}
 * + 真 Spring context wiring + 真实 {@link OutboundCfxEnvelopeBuilder} 装配链路）.
 *
 * <p><b>背景缺口（Plan §1.2 实测调用链 + commit 59ba4c7/76fe0fc 闭合）</b>：
 * {@code BizMessage2101InboundListener.enqueue9120Ack} 在 2101 inbound 后将
 * {@code messageType="9120"} + body={@code MsgReturn9120} 入
 * {@code outbound_message_queue}。consumer 侧
 * {@link OutboundCfxEnvelopeBuilder#build} 双查
 * {@code dispatcher.describeFor("9120")} +
 * {@code bodyClassRegistry.resolve("9120")} —— P4-MSG-I T1+T2 之前两查均抛
 * {@code OUTBOUND_5108_MSGNO_INVALID} / {@code OUTBOUND_5107_BODY_CLASS_NOT_FOUND}，
 * 致 9120 ack 永远无法上线，2101 模式 6 流断在装配段（PRD v1.3 §模式 6:863 强制 9120 ack）。</p>
 *
 * <p><b>既有覆盖盲区</b>：{@code Inbound2101WireTest} AC-3 仅断言
 * ack 入队（status=PENDING + body XML 持久化），<b>未</b>驱动
 * {@code OutboundQueueRunner → OutboundCfxEnvelopeBuilder.build("9120")}
 * 装配段，故缺口在 P4-MSG-I 之前从未被既有 IT 捕获。</p>
 *
 * <p><b>本 IT 的 P0 闭合证明语义</b>：
 * <ol>
 *   <li>显式以"9120" + {@code MsgReturn9120} body XML 调用 {@code build}，
 *       证明 dispatcher/registry 双查均通过（不抛 {@code OUTBOUND_5108/5107}）。</li>
 *   <li>断言产物 envelope 含 {@code <BatchHead9120>} 元素（BatchHead +
 *       ResponseBusinessHead 类目，{@code OutboundWireShapeDispatcher.BATCH_HEAD_RESPONSE_MSG_NOS}
 *       含 9120）+ {@code <MsgReturn9120>} body 元素 + {@code <MsgNo>9120</MsgNo>} CommonHead
 *       + {@code <Result>00000</Result>} 占位（ResponseBusinessHead requiresResultCode=true）。</li>
 *   <li>本 IT 在 T1+T2 之前会 FAIL（5107/5108）；T1+T2 后 PASS，作为缺口闭合的
 *       <b>回归基准</b>。T1+T2 已 ship，本 IT 直接 PASS 证明装配段已通。</li>
 * </ol></p>
 *
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}，由 Spring context 注入真实 bean。fixture
 * {@code BODY_XML_9120_ACK} 字段值满足 9120.xsd MsgReturn9120 sequence：
 * OriMsgNo MsgNo {@code length=4} numeric + Debug Text optional。envelope HEAD 由
 * 5-25 P0 fix 的 {@code BodyMsgIdGenerator} 注入 20 数字 MsgId + {@code CommonHeadComposer}
 * 注入 CorrMsgId 20 零 + App=HNDEMP；BatchHead9120 ResponseHead 含 Result="00000"
 * placeholder（{@code OutboundCfxEnvelopeBuilder} 装配段）。与 sibling
 * {@link OutboundEnvelopeXsdComplianceTest} 同 cache key（统一 ApplicationContext 复用）。</p>
 *
 * <p><b>命名沿用 sibling {@code Outbound1101WireTest} / {@code OutboundCommonForwardWireTest}
 * （{@code Test} 后缀以纳入 Surefire 默认 include {@code *Test.java}，
 * 红线 P2b-DEFECT-002 + CLAUDE.md 已知约束 / 主动披露 Plan §3 T5 表
 * {@code IT.java} 命名偏离）。</b></p>
 *
 * <p>PRD 依据: §3.2 报文结构 + §模式 6:863 2101 强制 9120 ack + §3.1.3 TLQ 消息属性。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Outbound9120AckEnvelopeBuilderTest {

    /** 9120 ack 业务体 — 最小满足 {@code MsgReturn9120} complexType sequence (OriMsgNo required, Debug optional)。 */
    private static final String BODY_XML_9120_ACK = "<MsgReturn9120>"
            + "<OriMsgNo>2101</OriMsgNo>"
            + "<Debug>mode-6-ack</Debug>"
            + "</MsgReturn9120>";

    /** {@code OutboundHeadFields.sendOrgCode} fixture — OrgCode length=14 实测约束。 */
    private static final String SEND_ORG_CODE = "BANK0010000001";

    /** {@code OutboundHeadFields.entrustDate} fixture — Date pattern yyyyMMdd 实测约束。 */
    private static final String ENTRUST_DATE = "20260519";

    /** {@code OutboundHeadFields.transitionNo} fixture — TransitionNo length=8 实测约束。 */
    private static final String TRANSITION_NO = "00002101";

    @Autowired
    private OutboundCfxEnvelopeBuilder builder;

    @Test
    @DisplayName("build 9120 ack envelope — P0 闭合 2101 模式 6 ack 装配段缺口（registry+dispatcher 双查通过）")
    void build9120AckEnvelope_shouldSucceedAfterT1T2Registration() {
        final OutboundMessageQueueEntity entity = givenAck9120Entity();
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        // 断言 1: build 不抛 FepBusinessException（registry/dispatcher 含 9120 后无 5107/5108）
        assertThatCode(() -> builder.build(entity, headFields))
                .as("build 9120 ack 应成功（T1+T2 注册后无 OUTBOUND_5107/5108）—— "
                        + "本 IT 在 T1+T2 之前会 FAIL，作为 2101 模式 6 ack 装配段缺口闭合的回归基准")
                .doesNotThrowAnyException();

        // 断言 2: envelope 结构含关键元素（装配段产物完整性）
        final String envelope = builder.build(entity, headFields).envelope();
        assertThat(envelope)
                .as("9120 ack envelope 必含 BatchHead9120（dispatcher.describeFor headElementName）")
                .contains("<BatchHead9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 MsgReturn9120 body 元素（bodyClassRegistry.resolve 反序列化 + marshal）")
                .contains("<MsgReturn9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 OriMsgNo=2101（MsgReturn9120 业务体字段穿透 marshal）")
                .contains("<OriMsgNo>2101</OriMsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope CommonHead 必含 MsgNo=9120（CommonHeadComposer.compose 注入 entity.messageType）")
                .contains("<MsgNo>9120</MsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope BatchHead9120 必含 Result 占位（ResponseBusinessHead requiresResultCode=true）")
                .contains("<Result>00000</Result>");
        assertThat(envelope)
                .as("9120 ack envelope 必含 CFX 外层封装（marshalToString 完整 envelope 产物）")
                .contains("</CFX>");
    }

    /**
     * 构造 9120 ack {@link OutboundMessageQueueEntity}，模拟
     * {@code BizMessage2101InboundListener.enqueue9120Ack} 入队产物。
     *
     * <p>{@code messageHeadXml} 字段在 {@code build} 内不被反序列化（{@code headFields}
     * 由 Runner 上层提供），合成占位即可（与 sibling {@code OutboundCfxEnvelopeBuilderTest}
     * 一致策略）。</p>
     *
     * @return ack entity fixture
     */
    private OutboundMessageQueueEntity givenAck9120Entity() {
        final OutboundMessageQueueEntity e = new OutboundMessageQueueEntity();
        e.setMessageType("9120");
        e.setMessageBodyXml(BODY_XML_9120_ACK);
        // messageHeadXml 不在 builder 内被反序列化（headFields 由 Runner 上层提供），合成占位
        e.setMessageHeadXml("<OutboundHeadFields/>");
        return e;
    }
}
